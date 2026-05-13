package com.desigual.camelgateway.processors.metrics;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("metricsProcessor")
public class MetricsProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        System.out.println("MetricsProcessor: en este processor se registran metricas para Prometheus y Grafana.");
    }
}
