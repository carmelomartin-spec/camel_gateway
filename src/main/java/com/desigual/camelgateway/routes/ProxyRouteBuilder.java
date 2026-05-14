package com.desigual.camelgateway.routes;

import com.desigual.camelgateway.config.GatewayProperties;
import com.desigual.camelgateway.model.config.AuditDefinition;
import com.desigual.camelgateway.model.config.MetricsDefinition;
import com.desigual.camelgateway.model.config.RateLimitDefinition;
import com.desigual.camelgateway.model.config.ServiceDefinition;
import com.desigual.camelgateway.processors.error.GatewayErrorCodes;
import com.desigual.camelgateway.processors.security.AuthorizationProcessor;
import com.desigual.camelgateway.processors.ratelimit.RateLimitProcessor;
import com.desigual.camelgateway.service.ServiceCatalog;
import io.micrometer.core.instrument.Tags;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.micrometer.MicrometerConstants;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.processor.ThrottlerRejectedExecutionException;
import org.springframework.stereotype.Component;

@Component
public class ProxyRouteBuilder extends RouteBuilder {

    private static final String PROPERTY_METRICS_ENABLED = "metricsEnabled";
    private static final String PROPERTY_AUDIT_ENABLED = "auditEnabled";
    private static final String PROPERTY_REQUEST_PATH = "requestPath";
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
            .setProperty(GatewayErrorCodes.PROPERTY_ERROR_CODE, constant(GatewayErrorCodes.RATE_LIMIT_EXCEEDED))
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
                    .process(this::setCompletionMetricTags)
                    .to("micrometer:counter:gateway.proxy.requests")
                    .to("micrometer:timer:gateway.proxy.duration?action=stop")
                    .removeHeader(MicrometerConstants.HEADER_METRIC_TAGS)
                    .removeHeader(HEADER_GATEWAY_METRIC_STATUS)
            .end()
            .choice()
                .when(exchangeProperty(PROPERTY_AUDIT_ENABLED).isEqualTo(true))
                    .to("bean:auditProcessor?method=prepare")
                    .to("sql:classpath:sql/insert-audit-event.sql")
            .end();

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
                    .setProperty(PROPERTY_REQUEST_PATH, constant(service.getExposure().getBasePath()))
                    .setProperty(PROPERTY_METRICS_ENABLED, constant(resolveMetricsEnabled(service)))
                    .setProperty(PROPERTY_AUDIT_ENABLED, constant(resolveAuditEnabled(service)))
                    .setHeader(Exchange.HTTP_METHOD, constant(service.getBackend().getMethod()));

                route.to("bean:auditProcessor?method=start");

                route.choice()
                    .when(exchangeProperty(PROPERTY_METRICS_ENABLED).isEqualTo(true))
                        .process(this::setStartMetricTags)
                        .to("micrometer:timer:gateway.proxy.duration?action=start")
                        .removeHeader(MicrometerConstants.HEADER_METRIC_TAGS)
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

    private void setStartMetricTags(Exchange exchange) {
        exchange.getMessage().setHeader(MicrometerConstants.HEADER_METRIC_TAGS, Tags.of(
            "serviceId", resolveTagValue(exchange.getProperty("serviceId", String.class)),
            "method", resolveTagValue(exchange.getMessage().getHeader(Exchange.HTTP_METHOD, String.class))
        ));
    }

    private void setCompletionMetricTags(Exchange exchange) {
        exchange.getMessage().setHeader(MicrometerConstants.HEADER_METRIC_TAGS, Tags.of(
            "serviceId", resolveTagValue(exchange.getProperty("serviceId", String.class)),
            "method", resolveTagValue(exchange.getMessage().getHeader(Exchange.HTTP_METHOD, String.class)),
            "status", resolveTagValue(exchange.getMessage().getHeader(HEADER_GATEWAY_METRIC_STATUS, String.class))
        ));
    }

    private String resolveTagValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private boolean resolveAuditEnabled(ServiceDefinition service) {
        AuditDefinition audit = service.getAudit();
        return audit != null && audit.getEnabled() != null
            ? audit.getEnabled()
            : gatewayProperties.getAudit().isEnabled();
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
