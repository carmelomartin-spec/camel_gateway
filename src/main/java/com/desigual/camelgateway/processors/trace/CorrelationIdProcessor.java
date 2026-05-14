package com.desigual.camelgateway.processors.trace;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("correlationIdProcessor")
public class CorrelationIdProcessor implements Processor {

    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String PROPERTY_CORRELATION_ID = "correlationId";

    @Override
    public void process(Exchange exchange) {
        System.out.printf("%-32s En este processor se garantiza que cada peticion tenga un correlation id.%n", "CorrelationIdProcessor:");

        String correlationId = exchange.getMessage().getHeader(HEADER_CORRELATION_ID, String.class);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        exchange.setProperty(PROPERTY_CORRELATION_ID, correlationId);
        exchange.getMessage().setHeader(HEADER_CORRELATION_ID, correlationId);
    }
}
