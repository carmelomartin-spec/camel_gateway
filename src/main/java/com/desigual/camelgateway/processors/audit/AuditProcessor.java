package com.desigual.camelgateway.processors.audit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("auditProcessor")
public class AuditProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        System.out.println("AuditProcessor: en este processor se registra la llamada en auditoria.");
    }
}
