package com.desigual.camelgateway.processors.config;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("effectiveConfigLoaderProcessor")
public class EffectiveConfigLoaderProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        System.out.printf("%-32s En este processor se carga la configuracion efectiva desde YAML, BBDD y Vault.%n","EffectiveConfigLoaderProcessor:");
    }
}
