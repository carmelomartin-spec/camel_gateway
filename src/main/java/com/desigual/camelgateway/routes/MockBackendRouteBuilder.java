package com.desigual.camelgateway.routes;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class MockBackendRouteBuilder extends RouteBuilder {

    @Override
    public void configure() {
        from("undertow:http://0.0.0.0:9090/clientes/demo?httpMethodRestrict=GET")
            .routeId("mock-backend-clientes-demo")
            .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
            .setBody(constant("""
                {
                  "customerId": "12345",
                  "fullName": "Cliente Demo",
                  "status": "ACTIVE",
                  "source": "mock-backend"
                }
                """));
    }
}
