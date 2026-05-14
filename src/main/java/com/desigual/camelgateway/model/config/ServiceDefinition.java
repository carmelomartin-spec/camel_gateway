package com.desigual.camelgateway.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceDefinition {

    private String id;
    private String name;
    private String status;
    private ExposureDefinition exposure;
    private BackendDefinition backend;

    @JsonProperty("rate_limit")
    private RateLimitDefinition rateLimit;

    private MetricsDefinition metrics;

    private AuditDefinition audit;

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

    public RateLimitDefinition getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitDefinition rateLimit) {
        this.rateLimit = rateLimit;
    }

    public MetricsDefinition getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsDefinition metrics) {
        this.metrics = metrics;
    }

    public AuditDefinition getAudit() {
        return audit;
    }

    public void setAudit(AuditDefinition audit) {
        this.audit = audit;
    }
}
