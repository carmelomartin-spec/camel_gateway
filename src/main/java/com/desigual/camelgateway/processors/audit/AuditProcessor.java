package com.desigual.camelgateway.processors.audit;

import com.desigual.camelgateway.processors.error.GatewayErrorCodes;
import com.desigual.camelgateway.processors.trace.CorrelationIdProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component("auditProcessor")
public class AuditProcessor implements Processor {

    public static final String HEADER_AUDIT_CORRELATION_ID = "AuditCorrelationId";
    public static final String HEADER_AUDIT_CONSUMER_ID = "AuditConsumerId";
    public static final String HEADER_AUDIT_SERVICE_ID = "AuditServiceId";
    public static final String HEADER_AUDIT_METHOD = "AuditMethod";
    public static final String HEADER_AUDIT_PATH = "AuditPath";
    public static final String HEADER_AUDIT_RESPONSE_CODE = "AuditResponseCode";
    public static final String HEADER_AUDIT_ELAPSED_MS = "AuditElapsedMs";
    public static final String HEADER_AUDIT_ERROR_CODE = "AuditErrorCode";
    public static final String HEADER_AUDIT_AUTHORIZED = "AuditAuthorized";
    public static final String HEADER_AUDIT_RATE_LIMITED = "AuditRateLimited";
    public static final String HEADER_AUDIT_CLIENT_IP = "AuditClientIp";
    public static final String HEADER_AUDIT_USER_AGENT = "AuditUserAgent";
    public static final String HEADER_AUDIT_BACKEND_ENDPOINT = "AuditBackendEndpoint";
    public static final String HEADER_AUDIT_REQUEST_SIZE = "AuditRequestSize";
    public static final String HEADER_AUDIT_RESPONSE_SIZE = "AuditResponseSize";

    private static final String HEADER_CONSUMER_ID = "X-Consumer-Id";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_REAL_IP = "X-Real-IP";
    private static final String HEADER_CAMEL_REMOTE_ADDRESS = "CamelHttpRemoteAddress";
    private static final String HEADER_AUTHORIZATION_ALLOWED = "AuthorizationAllowed";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String PROPERTY_SERVICE_ID = "serviceId";
    private static final String PROPERTY_REQUEST_PATH = "requestPath";
    private static final String PROPERTY_BACKEND_ENDPOINT = "backendEndpoint";
    private static final String PROPERTY_STARTED_AT = "auditStartedAt";
    private static final String PROPERTY_AUDIT_CONSUMER_ID = "auditConsumerId";
    private static final String PROPERTY_AUDIT_METHOD = "auditMethod";
    private static final String PROPERTY_AUDIT_PATH = "auditPath";
    private static final String PROPERTY_AUDIT_CLIENT_IP = "auditClientIp";
    private static final String PROPERTY_AUDIT_USER_AGENT = "auditUserAgent";
    private static final String PROPERTY_AUDIT_REQUEST_SIZE = "auditRequestSize";

    @Override
    public void process(Exchange exchange) {
        prepare(exchange);
    }

    public void start(Exchange exchange) {
        exchange.setProperty(PROPERTY_STARTED_AT, System.currentTimeMillis());
        exchange.setProperty(PROPERTY_AUDIT_CONSUMER_ID, exchange.getMessage().getHeader(HEADER_CONSUMER_ID, String.class));
        exchange.setProperty(PROPERTY_AUDIT_METHOD, exchange.getMessage().getHeader(Exchange.HTTP_METHOD, String.class));
        exchange.setProperty(PROPERTY_AUDIT_PATH, resolveRequestPath(exchange));
        exchange.setProperty(PROPERTY_AUDIT_CLIENT_IP, resolveClientIp(exchange));
        exchange.setProperty(PROPERTY_AUDIT_USER_AGENT, exchange.getMessage().getHeader(HEADER_USER_AGENT, String.class));
        exchange.setProperty(PROPERTY_AUDIT_REQUEST_SIZE, resolveMessageSize(exchange.getMessage().getBody()));
    }

    public void prepare(Exchange exchange) {
        System.out.printf("%-32s En este processor se preparan campos para auditoria.%n", "AuditProcessor:");

        exchange.getMessage().setHeader(HEADER_AUDIT_CORRELATION_ID, exchange.getProperty(
            CorrelationIdProcessor.PROPERTY_CORRELATION_ID,
            exchange.getExchangeId(),
            String.class
        ));
        exchange.getMessage().setHeader(HEADER_AUDIT_CONSUMER_ID, exchange.getProperty(PROPERTY_AUDIT_CONSUMER_ID, String.class));
        exchange.getMessage().setHeader(HEADER_AUDIT_SERVICE_ID, exchange.getProperty(PROPERTY_SERVICE_ID, String.class));
        exchange.getMessage().setHeader(HEADER_AUDIT_METHOD, exchange.getProperty(PROPERTY_AUDIT_METHOD, String.class));
        exchange.getMessage().setHeader(HEADER_AUDIT_PATH, exchange.getProperty(PROPERTY_AUDIT_PATH, String.class));
        exchange.getMessage().setHeader(HEADER_AUDIT_RESPONSE_CODE, resolveResponseCode(exchange));
        exchange.getMessage().setHeader(HEADER_AUDIT_ELAPSED_MS, resolveElapsedMillis(exchange));
        exchange.getMessage().setHeader(HEADER_AUDIT_ERROR_CODE, resolveErrorCode(exchange));
        exchange.getMessage().setHeader(HEADER_AUDIT_AUTHORIZED, resolveAuthorized(exchange));
        exchange.getMessage().setHeader(HEADER_AUDIT_RATE_LIMITED, resolveResponseCode(exchange) == 429);
        exchange.getMessage().setHeader(HEADER_AUDIT_CLIENT_IP, exchange.getProperty(PROPERTY_AUDIT_CLIENT_IP, String.class));
        exchange.getMessage().setHeader(HEADER_AUDIT_USER_AGENT, exchange.getProperty(PROPERTY_AUDIT_USER_AGENT, String.class));
        exchange.getMessage().setHeader(HEADER_AUDIT_BACKEND_ENDPOINT, exchange.getProperty(PROPERTY_BACKEND_ENDPOINT, String.class));
        exchange.getMessage().setHeader(HEADER_AUDIT_REQUEST_SIZE, exchange.getProperty(PROPERTY_AUDIT_REQUEST_SIZE, Integer.class));
        exchange.getMessage().setHeader(HEADER_AUDIT_RESPONSE_SIZE, resolveMessageSize(exchange.getMessage().getBody()));
    }

    private String resolveRequestPath(Exchange exchange) {
        String requestPath = exchange.getProperty(PROPERTY_REQUEST_PATH, String.class);
        if (requestPath == null || requestPath.isBlank()) {
            requestPath = exchange.getMessage().getHeader(Exchange.HTTP_PATH, String.class);
        }
        if (requestPath == null || requestPath.isBlank()) {
            requestPath = exchange.getMessage().getHeader(Exchange.HTTP_URI, String.class);
        }
        return requestPath;
    }

    private Integer resolveResponseCode(Exchange exchange) {
        Integer responseCode = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        return responseCode != null ? responseCode : 200;
    }

    private Long resolveElapsedMillis(Exchange exchange) {
        Long startedAt = exchange.getProperty(PROPERTY_STARTED_AT, Long.class);
        return startedAt != null ? System.currentTimeMillis() - startedAt : null;
    }

    private String resolveErrorCode(Exchange exchange) {
        Integer responseCode = resolveResponseCode(exchange);
        if (responseCode < 400) {
            return null;
        }

        String errorCode = exchange.getProperty(GatewayErrorCodes.PROPERTY_ERROR_CODE, String.class);
        if (errorCode != null && !errorCode.isBlank()) {
            return errorCode;
        }

        return GatewayErrorCodes.HTTP_ERROR;
    }

    private Boolean resolveAuthorized(Exchange exchange) {
        Integer responseCode = resolveResponseCode(exchange);
        if (responseCode == 401 || responseCode == 403) {
            return false;
        }

        Boolean authorizationAllowed = exchange.getMessage().getHeader(HEADER_AUTHORIZATION_ALLOWED, Boolean.class);
        if (authorizationAllowed != null) {
            return authorizationAllowed;
        }

        return responseCode < 400;
    }

    private Integer resolveMessageSize(Object body) {
        if (body == null) {
            return 0;
        }
        if (body instanceof byte[] bytes) {
            return bytes.length;
        }
        return body.toString().getBytes(StandardCharsets.UTF_8).length;
    }

    private String resolveClientIp(Exchange exchange) {
        String clientIp = exchange.getMessage().getHeader(HEADER_FORWARDED_FOR, String.class);
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = exchange.getMessage().getHeader(HEADER_REAL_IP, String.class);
        }
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = exchange.getMessage().getHeader(HEADER_CAMEL_REMOTE_ADDRESS, String.class);
        }
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        return clientIp.split(",", 2)[0].trim();
    }
}
