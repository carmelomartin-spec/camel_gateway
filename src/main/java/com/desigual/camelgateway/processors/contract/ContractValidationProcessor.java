package com.desigual.camelgateway.processors.contract;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("contractValidationProcessor")
public class ContractValidationProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        System.out.printf("%-32s En este processor se valida la peticion contra el contrato publicado por el proxy.%n", "ContractValidationProcessor:");
    }
}
