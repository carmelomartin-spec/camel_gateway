package com.desigual.camelgateway.service;

import com.desigual.camelgateway.model.config.ServiceDefinition;

import java.util.List;

public class ServiceCatalog {

    private final List<ServiceDefinition> services;

    public ServiceCatalog(List<ServiceDefinition> services) {
        this.services = List.copyOf(services);
    }

    public List<ServiceDefinition> getServices() {
        return services;
    }
}
