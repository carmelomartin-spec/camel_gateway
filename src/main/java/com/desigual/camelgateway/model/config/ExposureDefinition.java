package com.desigual.camelgateway.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ExposureDefinition {

    @JsonProperty("base_path")
    private String basePath;

    private List<String> methods;

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }
}
