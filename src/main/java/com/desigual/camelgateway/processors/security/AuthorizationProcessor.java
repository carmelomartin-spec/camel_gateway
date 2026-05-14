package com.desigual.camelgateway.processors.security;

import com.desigual.camelgateway.config.GatewayProperties;
import com.desigual.camelgateway.processors.error.GatewayErrorCodes;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component("authorizationProcessor")
public class AuthorizationProcessor implements Processor {

    public static final String HEADER_AUTHORIZATION_CONSUMER_ID = "AuthorizationConsumerId";
    public static final String HEADER_AUTHORIZATION_SERVICE_ID = "AuthorizationServiceId";
    public static final String HEADER_AUTHORIZATION_METHOD = "AuthorizationMethod";
    public static final String HEADER_AUTHORIZATION_ALLOWED = "AuthorizationAllowed";

    private static final String HEADER_CONSUMER_ID = "X-Consumer-Id";
    private static final String PROPERTY_CONSUMER_ID = "consumerId";
    private static final String PROPERTY_SERVICE_ID = "serviceId";

    private final boolean enabled;

    public AuthorizationProcessor(GatewayProperties gatewayProperties) {
        this.enabled = gatewayProperties.getAuthorization().isEnabled();
    }

    @Override
    public void process(Exchange exchange) {
        prepare(exchange);
    }

    public void prepare(Exchange exchange) {
        if (!enabled) {
            exchange.getMessage().setHeader(HEADER_AUTHORIZATION_ALLOWED, true);
            return;
        }

        String consumerId = resolveConsumerId(exchange);
        if (consumerId == null || consumerId.isBlank()) {
            reject(exchange, 401, "unauthorized", "Missing consumer identity");
            return;
        }

        String serviceId = exchange.getProperty(PROPERTY_SERVICE_ID, String.class);
        if (serviceId == null || serviceId.isBlank()) {
            reject(exchange, 403, "forbidden", "Missing service context");
            return;
        }

        String method = exchange.getMessage().getHeader(Exchange.HTTP_METHOD, String.class);
        exchange.getMessage().setHeader(HEADER_AUTHORIZATION_CONSUMER_ID, consumerId);
        exchange.getMessage().setHeader(HEADER_AUTHORIZATION_SERVICE_ID, serviceId);
        exchange.getMessage().setHeader(HEADER_AUTHORIZATION_METHOD, normalizeMethod(method));
    }

    public void enforce(Exchange exchange) {
        if (!enabled || exchange.isRouteStop()) {
            return;
        }

        Boolean allowed = exchange.getMessage().getHeader(HEADER_AUTHORIZATION_ALLOWED, Boolean.class);
        if (!Boolean.TRUE.equals(allowed)) {
            reject(exchange, 403, "forbidden", "Consumer is not authorized for this service");
        }
    }

    private String resolveConsumerId(Exchange exchange) {
        String consumerId = exchange.getProperty(PROPERTY_CONSUMER_ID, String.class);
        if (consumerId == null || consumerId.isBlank()) {
            consumerId = exchange.getMessage().getHeader(HEADER_CONSUMER_ID, String.class);
        }
        return consumerId;
    }

    private String normalizeMethod(String method) {
        if (method == null || method.isBlank()) {
            return "";
        }
        return method.trim().toUpperCase(Locale.ROOT);
    }

    private void reject(Exchange exchange, int statusCode, String error, String message) {
        exchange.setProperty(GatewayErrorCodes.PROPERTY_ERROR_CODE, error);
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getMessage().setBody("""
            {
              "error": "%s",
              "message": "%s"
            }
            """.formatted(error, message));
        exchange.setRouteStop(true);
    }
}
