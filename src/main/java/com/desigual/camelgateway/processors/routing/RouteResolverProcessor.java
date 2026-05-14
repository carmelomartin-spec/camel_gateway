package com.desigual.camelgateway.processors.routing;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("routeResolverProcessor")
public class RouteResolverProcessor implements Processor {

    private static final String PROPERTY_SERVICE_ID = "serviceId";
    private static final String PROPERTY_BACKEND_TYPE = "backendType";
    private static final String PROPERTY_BACKEND_ENDPOINT = "backendEndpoint";

    @Override
    public void process(Exchange exchange) {
        String serviceId = exchange.getProperty(PROPERTY_SERVICE_ID, String.class);
        String backendType = exchange.getProperty(PROPERTY_BACKEND_TYPE, String.class);
        String backendEndpoint = exchange.getProperty(PROPERTY_BACKEND_ENDPOINT, String.class);

        if (isBlank(serviceId) || isBlank(backendType) || isBlank(backendEndpoint)) {
            throw new IllegalStateException("Missing route context for service: serviceId=%s, backendType=%s, backendEndpoint=%s"
                .formatted(serviceId, backendType, backendEndpoint));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
