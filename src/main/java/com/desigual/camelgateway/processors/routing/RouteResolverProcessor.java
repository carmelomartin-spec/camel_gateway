package com.desigual.camelgateway.processors.routing;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("routeResolverProcessor")
public class RouteResolverProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        System.out.printf("%-32s En este processor se resuelve el servicio logico a partir del metodo y path HTTP.%n", "RouteResolverProcessor:");
    }
}
