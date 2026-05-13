# camel_gateway

Estructura base para una capa proxy JSON con Apache Camel entre n8n y servicios internos TIBCO / .NET.

## Estructura

```text
camel_gateway/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/desigual/camelgateway/
│   │   │       ├── CamelGatewayApplication.java
│   │   │       ├── routes/
│   │   │       │   └── ProxyRouteBuilder.java
│   │   │       ├── processors/
│   │   │       │   ├── trace/
│   │   │       │   ├── routing/
│   │   │       │   ├── config/
│   │   │       │   ├── security/
│   │   │       │   ├── ratelimit/
│   │   │       │   ├── contract/
│   │   │       │   ├── mapping/
│   │   │       │   ├── masking/
│   │   │       │   ├── audit/
│   │   │       │   ├── metrics/
│   │   │       │   └── error/
│   │   │       ├── model/
│   │   │       │   ├── config/
│   │   │       │   ├── audit/
│   │   │       │   └── security/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       └── exception/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── config/
│   │           ├── environments/
│   │           │   └── local/
│   │           │       ├── services/
│   │           │       ├── policies/
│   │           │       └── mappings/
│   │           └── contracts/
│   │               ├── proxy/
│   │               └── backend/
│   └── test/
│       └── java/
│           └── com/desigual/camelgateway/
```

## Flujo previsto

```text
correlationIdProcessor
routeResolverProcessor
effectiveConfigLoaderProcessor
authProcessor
authorizationProcessor
rateLimitProcessor
contractValidationProcessor
requestMappingProcessor
backend invocation
responseMappingProcessor
maskingProcessor
auditProcessor
metricsProcessor
```

## Scripts

```bash
scripts/build.sh
scripts/run.sh
scripts/build-and-run.sh
```
