package com.desigual.camelgateway.processors.routing;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("routeResolverProcessor")
public class RouteResolverProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        // TODO Resolve logical service from HTTP method and path.
    }
}
