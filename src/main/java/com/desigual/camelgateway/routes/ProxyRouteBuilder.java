package com.desigual.camelgateway.routes;

import com.desigual.camelgateway.config.GatewayProperties;
import com.desigual.camelgateway.model.config.MetricsDefinition;
import com.desigual.camelgateway.model.config.RateLimitDefinition;
import com.desigual.camelgateway.model.config.ServiceDefinition;
import com.desigual.camelgateway.processors.security.AuthorizationProcessor;
import com.desigual.camelgateway.processors.ratelimit.RateLimitProcessor;
import com.desigual.camelgateway.service.ServiceCatalog;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.processor.ThrottlerRejectedExecutionException;
import org.springframework.stereotype.Component;

@Component
public class ProxyRouteBuilder extends RouteBuilder {

    private static final String PROPERTY_METRICS_ENABLED = "metricsEnabled";
    private static final String HEADER_GATEWAY_METRIC_STATUS = "GatewayMetricStatus";

    private final ServiceCatalog serviceCatalog;
    private final GatewayProperties gatewayProperties;
    private final String proxyHost;
    private final int proxyPort;

    public ProxyRouteBuilder(
        ServiceCatalog serviceCatalog,
        GatewayProperties gatewayProperties
    ) {
        this.serviceCatalog = serviceCatalog;
        this.gatewayProperties = gatewayProperties;
        this.proxyHost = gatewayProperties.getProxy().getHost();
        this.proxyPort = gatewayProperties.getProxy().getPort();
    }

    @Override
    public void configure() {
        onException(ThrottlerRejectedExecutionException.class)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(429))
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setBody(constant("""
                {
                  "error": "rate_limit_exceeded",
                  "message": "Too many requests"
                }
                """))
            .to("bean:maskingProcessor");

        onException(Exception.class)
            .handled(true)
            .to("bean:errorNormalizerProcessor")
            .to("bean:maskingProcessor");

        onCompletion()
            .choice()
                .when(exchangeProperty(PROPERTY_METRICS_ENABLED).isEqualTo(true))
                    .setHeader(HEADER_GATEWAY_METRIC_STATUS, simple("${header.CamelHttpResponseCode}"))
                    .choice()
                        .when(header(HEADER_GATEWAY_METRIC_STATUS).isNull())
                            .setHeader(HEADER_GATEWAY_METRIC_STATUS, constant("200"))
                    .end()
                    .toD("micrometer:counter:gateway.proxy.requests"
                        + "?tags=serviceId=${exchangeProperty.serviceId},method=${header.CamelHttpMethod},status=${header."
                        + HEADER_GATEWAY_METRIC_STATUS + "}")
                    .toD("micrometer:timer:gateway.proxy.duration"
                        + "?action=stop&tags=serviceId=${exchangeProperty.serviceId},method=${header.CamelHttpMethod},status=${header."
                        + HEADER_GATEWAY_METRIC_STATUS + "}")
                    .removeHeader(HEADER_GATEWAY_METRIC_STATUS)
            .end()
            .to("bean:auditProcessor");

        for (ServiceDefinition service : serviceCatalog.getServices()) {
            if (!"active".equalsIgnoreCase(service.getStatus())) {
                continue;
            }

            for (String method : service.getExposure().getMethods()) {
                RouteDefinition route = from(buildUndertowUri(service, method))
                    .routeId("proxy-" + service.getId() + "-" + method.toLowerCase())
                    .setProperty("serviceId", constant(service.getId()))
                    .setProperty("backendType", constant(service.getBackend().getType()))
                    .setProperty("backendEndpoint", constant(service.getBackend().getEndpointUrl()))
                    .setProperty(PROPERTY_METRICS_ENABLED, constant(resolveMetricsEnabled(service)))
                    .setHeader(Exchange.HTTP_METHOD, constant(service.getBackend().getMethod()));

                route.choice()
                    .when(exchangeProperty(PROPERTY_METRICS_ENABLED).isEqualTo(true))
                        .toD("micrometer:timer:gateway.proxy.duration"
                            + "?action=start&tags=serviceId=${exchangeProperty.serviceId},method=${header.CamelHttpMethod}")
                    .end();

                route
                    .to("bean:correlationIdProcessor")
                    .to("bean:routeResolverProcessor")
                    .to("bean:effectiveConfigLoaderProcessor")
                    .to("bean:authProcessor")
                    .to("bean:authorizationProcessor?method=prepare");

                route.choice()
                    .when(header(AuthorizationProcessor.HEADER_AUTHORIZATION_ALLOWED).isEqualTo(true))
                        .log("Authorization disabled or pre-approved for ${exchangeProperty.serviceId}")
                    .otherwise()
                        .to("sql:classpath:sql/authorize-consumer.sql?outputType=SelectOne&outputHeader="
                            + AuthorizationProcessor.HEADER_AUTHORIZATION_ALLOWED)
                        .to("bean:authorizationProcessor?method=enforce")
                    .end();

                route.to("bean:rateLimitProcessor");

                EffectiveRateLimit rateLimit = resolveRateLimit(service);
                if (rateLimit.enabled() && rateLimit.requests() > 0) {
                    route.throttle(rateLimit.requests())
                        .timePeriodMillis(rateLimit.windowMillis())
                        .rejectExecution(true)
                        .correlationExpression(header(RateLimitProcessor.HEADER_RATE_LIMIT_CONSUMER));
                }

                route
                    .to("bean:contractValidationProcessor")
                    .to("bean:requestMappingProcessor")
                    .toD("${exchangeProperty.backendEndpoint}")
                    .to("bean:responseMappingProcessor")
                    .to("bean:maskingProcessor");
            }
        }
    }

    private boolean resolveMetricsEnabled(ServiceDefinition service) {
        MetricsDefinition metrics = service.getMetrics();
        return metrics != null && metrics.getEnabled() != null
            ? metrics.getEnabled()
            : gatewayProperties.getMetrics().isEnabled();
    }

    private String buildUndertowUri(ServiceDefinition service, String method) {
        return "undertow:http://" + proxyHost + ":" + proxyPort
            + service.getExposure().getBasePath()
            + "?httpMethodRestrict="
            + method;
    }

    private EffectiveRateLimit resolveRateLimit(ServiceDefinition service) {
        RateLimitDefinition rateLimit = service.getRateLimit();
        GatewayProperties.RateLimit defaultRateLimit = gatewayProperties.getRateLimit();

        boolean enabled = rateLimit != null && rateLimit.getEnabled() != null
            ? rateLimit.getEnabled()
            : defaultRateLimit.isEnabled();
        int requests = rateLimit != null && rateLimit.getRequests() != null
            ? rateLimit.getRequests()
            : defaultRateLimit.getRequests();
        long windowSeconds = rateLimit != null && rateLimit.getWindowSeconds() != null
            ? rateLimit.getWindowSeconds()
            : defaultRateLimit.getWindowSeconds();

        return new EffectiveRateLimit(enabled, requests, windowSeconds);
    }

    private record EffectiveRateLimit(boolean enabled, int requests, long windowSeconds) {

        private EffectiveRateLimit {
            windowSeconds = Math.max(1, windowSeconds);
        }

        private long windowMillis() {
            return windowSeconds * 1000;
        }
    }
}
