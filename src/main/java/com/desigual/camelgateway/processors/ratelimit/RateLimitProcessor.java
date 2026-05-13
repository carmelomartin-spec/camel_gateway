package com.desigual.camelgateway.processors.ratelimit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("rateLimitProcessor")
public class RateLimitProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        // TODO Check consumer and service rate limits.
    }
}
