package com.desigual.camelgateway.processors.contract;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("contractValidationProcessor")
public class ContractValidationProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        // TODO Validate request body against the proxy contract.
    }
}
