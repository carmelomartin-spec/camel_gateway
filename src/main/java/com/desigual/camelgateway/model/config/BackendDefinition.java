package com.desigual.camelgateway.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackendDefinition {

    private String type;
    private String method;

    @JsonProperty("endpoint_url")
    private String endpointUrl;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }
}
