package com.desigual.camelgateway.processors.mapping;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("requestMappingProcessor")
public class RequestMappingProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        System.out.println("RequestMappingProcessor: en este processor se transforma la peticion del proxy al formato esperado por el backend.");
    }
}
