package com.desigual.camelgateway.processors.mapping;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("requestMappingProcessor")
public class RequestMappingProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        exchange.getMessage().removeHeader(Exchange.HTTP_URI);
        exchange.getMessage().removeHeader(Exchange.HTTP_PATH);
        exchange.getMessage().removeHeader(Exchange.HTTP_QUERY);
    }
}
