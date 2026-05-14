package com.desigual.camelgateway.processors.error;

public final class GatewayErrorCodes {

    public static final String PROPERTY_ERROR_CODE = "errorCode";

    public static final String UNAUTHORIZED = "unauthorized";
    public static final String FORBIDDEN = "forbidden";
    public static final String RATE_LIMIT_EXCEEDED = "rate_limit_exceeded";
    public static final String GATEWAY_ERROR = "gateway_error";
    public static final String HTTP_ERROR = "http_error";

    private GatewayErrorCodes() {
    }
}
