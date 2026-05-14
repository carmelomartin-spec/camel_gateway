package com.desigual.camelgateway.processors.ratelimit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("rateLimitProcessor")
public class RateLimitProcessor implements Processor {

    public static final String HEADER_RATE_LIMIT_CONSUMER = "X-RateLimit-Consumer";

    private static final String DEFAULT_CONSUMER = "anonymous";
    private static final String HEADER_CONSUMER_ID = "X-Consumer-Id";
    private static final String HEADER_API_KEY = "X-Api-Key";
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_REAL_IP = "X-Real-IP";
    private static final String HEADER_CAMEL_REMOTE_ADDRESS = "CamelHttpRemoteAddress";

    @Override
    public void process(Exchange exchange) {
        exchange.getMessage().setHeader(HEADER_RATE_LIMIT_CONSUMER, resolveConsumerId(exchange));
    }

    private String resolveConsumerId(Exchange exchange) {
        String consumerId = exchange.getMessage().getHeader(HEADER_CONSUMER_ID, String.class);
        if (consumerId == null || consumerId.isBlank()) {
            consumerId = exchange.getMessage().getHeader(HEADER_API_KEY, String.class);
        }
        if (consumerId == null || consumerId.isBlank()) {
            consumerId = exchange.getMessage().getHeader(HEADER_FORWARDED_FOR, String.class);
        }
        if (consumerId == null || consumerId.isBlank()) {
            consumerId = exchange.getMessage().getHeader(HEADER_REAL_IP, String.class);
        }
        if (consumerId == null || consumerId.isBlank()) {
            consumerId = exchange.getMessage().getHeader(HEADER_CAMEL_REMOTE_ADDRESS, String.class);
        }
        if (consumerId == null || consumerId.isBlank()) {
            return DEFAULT_CONSUMER;
        }
        return consumerId.split(",", 2)[0].trim();
    }
}
