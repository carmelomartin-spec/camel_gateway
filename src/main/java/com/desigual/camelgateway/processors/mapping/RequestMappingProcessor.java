package com.desigual.camelgateway.processors.mapping;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("requestMappingProcessor")
public class RequestMappingProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        // TODO Transform proxy request JSON into backend request JSON.
    }
}
