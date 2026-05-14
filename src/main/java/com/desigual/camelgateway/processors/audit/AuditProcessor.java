package com.desigual.camelgateway.processors.audit;

import com.desigual.camelgateway.processors.trace.CorrelationIdProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

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
    public static final String HEADER_AUDIT_CLIENT_IP = "AuditClientIp";
    public static final String HEADER_AUDIT_USER_AGENT = "AuditUserAgent";
    public static final String HEADER_AUDIT_BACKEND_ENDPOINT = "AuditBackendEndpoint";

    private static final String HEADER_CONSUMER_ID = "X-Consumer-Id";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_REAL_IP = "X-Real-IP";
    private static final String HEADER_CAMEL_REMOTE_ADDRESS = "CamelHttpRemoteAddress";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String PROPERTY_SERVICE_ID = "serviceId";
    private static final String PROPERTY_BACKEND_ENDPOINT = "backendEndpoint";
    private static final String PROPERTY_STARTED_AT = "auditStartedAt";

    @Override
    public void process(Exchange exchange) {
        prepare(exchange);
    }

    public void start(Exchange exchange) {
        exchange.setProperty(PROPERTY_STARTED_AT, System.currentTimeMillis());
    }

    public void prepare(Exchange exchange) {
        System.out.printf("%-32s En este processor se preparan campos para auditoria.%n", "AuditProcessor:");

        exchange.getMessage().setHeader(HEADER_AUDIT_CORRELATION_ID, exchange.getProperty(
            CorrelationIdProcessor.PROPERTY_CORRELATION_ID,
            exchange.getExchangeId(),
            String.class
        ));
        exchange.getMessage().setHeader(HEADER_AUDIT_CONSUMER_ID, exchange.getMessage().getHeader(HEADER_CONSUMER_ID, String.class));
        exchange.getMessage().setHeader(HEADER_AUDIT_SERVICE_ID, exchange.getProperty(PROPERTY_SERVICE_ID, String.class));
        exchange.getMessage().setHeader(HEADER_AUDIT_METHOD, exchange.getMessage().getHeader(Exchange.HTTP_METHOD, String.class));
        exchange.getMessage().setHeader(HEADER_AUDIT_PATH, exchange.getMessage().getHeader(Exchange.HTTP_PATH, String.class));
        exchange.getMessage().setHeader(HEADER_AUDIT_RESPONSE_CODE, resolveResponseCode(exchange));
        exchange.getMessage().setHeader(HEADER_AUDIT_ELAPSED_MS, resolveElapsedMillis(exchange));
        exchange.getMessage().setHeader(HEADER_AUDIT_ERROR_CODE, resolveErrorCode(exchange));
        exchange.getMessage().setHeader(HEADER_AUDIT_CLIENT_IP, resolveClientIp(exchange));
        exchange.getMessage().setHeader(HEADER_AUDIT_USER_AGENT, exchange.getMessage().getHeader(HEADER_USER_AGENT, String.class));
        exchange.getMessage().setHeader(HEADER_AUDIT_BACKEND_ENDPOINT, exchange.getProperty(PROPERTY_BACKEND_ENDPOINT, String.class));
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
        return responseCode >= 400 ? exchange.getMessage().getHeader("CamelFailureEndpoint", String.class) : null;
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
            return null;
        }
        return clientIp.split(",", 2)[0].trim();
    }
}
