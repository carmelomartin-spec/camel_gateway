package com.desigual.camelgateway.processors.mapping;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("responseMappingProcessor")
public class ResponseMappingProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        // TODO Transform backend response JSON into proxy response JSON.
    }
}
