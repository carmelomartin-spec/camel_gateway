package com.desigual.camelgateway.processors.error;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("errorNormalizerProcessor")
public class ErrorNormalizerProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        // TODO Normalize exceptions into controlled HTTP responses.
    }
}
