package com.desigual.camelgateway.processors.metrics;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("metricsProcessor")
public class MetricsProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        // TODO Register request metrics for Prometheus/Grafana.
    }
}
