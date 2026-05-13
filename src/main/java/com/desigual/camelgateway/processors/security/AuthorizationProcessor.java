package com.desigual.camelgateway.processors.security;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("authorizationProcessor")
public class AuthorizationProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        // TODO Authorize consumer access to the resolved service.
    }
}
