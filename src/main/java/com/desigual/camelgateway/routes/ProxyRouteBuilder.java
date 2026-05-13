package com.desigual.camelgateway.routes;

import com.desigual.camelgateway.config.GatewayProperties;
import com.desigual.camelgateway.model.config.RateLimitDefinition;
import com.desigual.camelgateway.model.config.ServiceDefinition;
import com.desigual.camelgateway.processors.ratelimit.RateLimitProcessor;
import com.desigual.camelgateway.service.ServiceCatalog;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.processor.ThrottlerRejectedExecutionException;
import org.springframework.stereotype.Component;

@Component
public class ProxyRouteBuilder extends RouteBuilder {

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
            .to("bean:auditProcessor")
            .to("bean:metricsProcessor");

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
                    .setHeader(Exchange.HTTP_METHOD, constant(service.getBackend().getMethod()))
                    .to("bean:correlationIdProcessor")
                    .to("bean:routeResolverProcessor")
                    .to("bean:effectiveConfigLoaderProcessor")
                    .to("bean:authProcessor")
                    .to("bean:authorizationProcessor")
                    .to("bean:rateLimitProcessor");

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
