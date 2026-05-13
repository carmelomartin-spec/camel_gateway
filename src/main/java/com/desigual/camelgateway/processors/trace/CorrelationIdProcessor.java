package com.desigual.camelgateway.processors.trace;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("correlationIdProcessor")
public class CorrelationIdProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        System.out.println("CorrelationIdProcessor: en este processor se garantiza que cada peticion tenga un correlation id.");
    }
}
