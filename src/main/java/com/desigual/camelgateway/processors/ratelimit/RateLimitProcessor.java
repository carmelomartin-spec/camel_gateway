package com.desigual.camelgateway.processors.ratelimit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("rateLimitProcessor")
public class RateLimitProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        System.out.println("RateLimitProcessor: en este processor se comprueban los limites de uso del consumidor y del servicio.");
    }
}
