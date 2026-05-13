package com.desigual.camelgateway.routes;

import com.desigual.camelgateway.model.config.ServiceDefinition;
import com.desigual.camelgateway.service.ServiceCatalog;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProxyRouteBuilder extends RouteBuilder {

    private final ServiceCatalog serviceCatalog;
    private final String proxyHost;
    private final int proxyPort;

    public ProxyRouteBuilder(
        ServiceCatalog serviceCatalog,
        @Value("${gateway.proxy.host:0.0.0.0}") String proxyHost,
        @Value("${gateway.proxy.port:8080}") int proxyPort
    ) {
        this.serviceCatalog = serviceCatalog;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    @Override
    public void configure() {
        onException(Exception.class)
            .handled(true)
            .to("bean:errorNormalizerProcessor")
            .to("bean:maskingProcessor");

        onCompletion()
            .to("bean:auditProcessor")
            .to("bean:metricsProcessor");

        for (ServiceDefinition service : serviceCatalog.getServices()) {
            if (!"active".equalsIgnoreCase(service.getStatus())) {
                continue;
            }

            for (String method : service.getExposure().getMethods()) {
                from(buildUndertowUri(service, method))
                    .routeId("proxy-" + service.getId() + "-" + method.toLowerCase())
                    .setProperty("serviceId", constant(service.getId()))
                    .setProperty("backendType", constant(service.getBackend().getType()))
                    .setProperty("backendEndpoint", constant(service.getBackend().getEndpointUrl()))
                    .setHeader(Exchange.HTTP_METHOD, constant(service.getBackend().getMethod()))
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
                    .to("bean:maskingProcessor");
            }
        }
    }

    private String buildUndertowUri(ServiceDefinition service, String method) {
        return "undertow:http://" + proxyHost + ":" + proxyPort
            + service.getExposure().getBasePath()
            + "?httpMethodRestrict="
            + method;
    }
}
