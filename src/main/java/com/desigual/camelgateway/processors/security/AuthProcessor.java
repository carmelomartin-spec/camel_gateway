package com.desigual.camelgateway.processors.security;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("authProcessor")
public class AuthProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
    }
}
