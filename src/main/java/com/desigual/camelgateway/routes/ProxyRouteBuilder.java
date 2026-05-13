package com.desigual.camelgateway.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ProxyRouteBuilder extends RouteBuilder {

    @Override
    public void configure() {
        /*
        onException(Exception.class)
            .handled(true)
            .to("bean:errorNormalizerProcessor")
            .to("bean:maskingProcessor")
            .to("bean:auditProcessor")
            .to("bean:metricsProcessor");

        from("undertow:http://0.0.0.0:8080/api/v1/clientes/busqueda?httpMethodRestrict=POST")
            .routeId("proxy-clientes-busqueda-v1")
            .to("bean:correlationIdProcessor")
            .to("bean:routeResolverProcessor")
            .to("bean:effectiveConfigLoaderProcessor")
            .to("bean:authProcessor")
            .to("bean:authorizationProcessor")
            .to("bean:rateLimitProcessor")
            .to("bean:contractValidationProcessor")
            .to("bean:requestMappingProcessor")
            .toD("${exchangeProperty.backendEndpoint}")
            .to("bean:responseMappingProcessor")
            .to("bean:maskingProcessor")
            .to("bean:auditProcessor")
            .to("bean:metricsProcessor");
        */
    }
}
