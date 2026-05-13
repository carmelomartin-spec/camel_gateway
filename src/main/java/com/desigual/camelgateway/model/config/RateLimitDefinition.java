package com.desigual.camelgateway.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RateLimitDefinition {

    private Boolean enabled;

    private Integer requests;

    @JsonProperty("window_seconds")
    private Long windowSeconds;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getRequests() {
        return requests;
    }

    public void setRequests(Integer requests) {
        this.requests = requests;
    }

    public Long getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(Long windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
}
