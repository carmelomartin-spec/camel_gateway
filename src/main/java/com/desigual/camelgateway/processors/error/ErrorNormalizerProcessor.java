package com.desigual.camelgateway.processors.error;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component("errorNormalizerProcessor")
public class ErrorNormalizerProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        System.out.printf("%-32s En este processor se normalizan errores a respuestas HTTP controladas.%n", "ErrorNormalizerProcessor:");

        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        String message = exception != null ? exception.getMessage() : "Unexpected gateway error";

        exchange.setProperty(GatewayErrorCodes.PROPERTY_ERROR_CODE, GatewayErrorCodes.GATEWAY_ERROR);
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 502);
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getMessage().setBody("""
            {
              "error": "gateway_error",
              "message": "%s"
            }
            """.formatted(escapeJson(message)));
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
