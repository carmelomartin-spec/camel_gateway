package com.desigual.camelgateway.model.config;

public class ServiceDefinition {

    private String id;
    private String name;
    private String status;
    private ExposureDefinition exposure;
    private BackendDefinition backend;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ExposureDefinition getExposure() {
        return exposure;
    }

    public void setExposure(ExposureDefinition exposure) {
        this.exposure = exposure;
    }

    public BackendDefinition getBackend() {
        return backend;
    }

    public void setBackend(BackendDefinition backend) {
        this.backend = backend;
    }
}
