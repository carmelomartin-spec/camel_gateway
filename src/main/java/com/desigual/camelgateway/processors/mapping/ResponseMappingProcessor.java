package com.desigual.camelgateway.processors.mapping;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("responseMappingProcessor")
public class ResponseMappingProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        System.out.println("ResponseMappingProcessor: en este processor se transforma la respuesta del backend al contrato externo del proxy.");
    }
}
