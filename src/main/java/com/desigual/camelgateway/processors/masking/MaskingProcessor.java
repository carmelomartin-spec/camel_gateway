package com.desigual.camelgateway.processors.masking;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("maskingProcessor")
public class MaskingProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
    }
}
