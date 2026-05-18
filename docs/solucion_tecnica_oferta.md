# Solucion tecnica propuesta para Camel Gateway

**Cliente:** Desigual  
**Proyecto:** Capa proxy JSON entre n8n y servicios internos TIBCO / .NET  
**Documento:** Descripcion tecnica para oferta  
**Version:** 1.0  
**Fecha:** 18/05/2026

---

## Pagina 1 de 15 - Resumen ejecutivo

La solucion propuesta consiste en una pasarela tecnica ligera, extensible y gobernable basada en Spring Boot y Apache Camel. Su objetivo es exponer un punto de entrada JSON estable para consumidores como n8n y otros clientes internos, desacoplando dichos consumidores de la complejidad, ubicacion y evolucion de los servicios backend existentes, principalmente servicios TIBCO y .NET.

El componente se plantea como una capa de mediacion. No sustituye los sistemas internos ni replica logica de negocio. Su funcion es resolver el encaminamiento, aplicar controles transversales, normalizar errores, registrar auditoria, generar metricas y permitir que cada servicio publicado se gobierne mediante configuracion. Esto reduce el acoplamiento entre integraciones, mejora la trazabilidad operacional y facilita la incorporacion progresiva de nuevos endpoints sin tener que construir una aplicacion especifica para cada integracion.

La implementacion parte de un proyecto Java con Spring Boot 3.5.x, Apache Camel 4.14.x, Actuator, Micrometer, Prometheus y PostgreSQL. La aplicacion arranca como un servicio independiente y crea rutas de proxy a partir de ficheros YAML por entorno. Cada definicion de servicio incluye identificador, ruta expuesta, metodos HTTP permitidos, backend objetivo, politica de rate limit, auditoria y metricas.

La solucion incorpora desde el inicio capacidades necesarias para un entorno empresarial: correlacion de peticiones, autorizacion por consumidor y servicio, limitacion de peticiones, insercion de eventos de auditoria en base de datos, metricas tecnicas consultables por Actuator/Prometheus, normalizacion de errores y preparacion para validacion contractual y transformaciones de entrada/salida.

El enfoque propuesto es incremental. La primera entrega consolida la pasarela operativa, el modelo de configuracion, las rutas dinamicas, los controles de seguridad basicos, la auditoria y la observabilidad. Las siguientes fases completan validacion de contratos, mascarado de datos sensibles, mapeos avanzados, despliegue productivo, hardening y automatizacion CI/CD.

El resultado esperado es una pieza reutilizable para acelerar integraciones. Para cada nuevo servicio, el equipo podra registrar una definicion YAML, configurar permisos en base de datos, habilitar limites y metricas, y publicar la ruta sin modificar el nucleo de la pasarela salvo que aparezcan necesidades transversales nuevas.

<div style="page-break-after: always;"></div>

## Pagina 2 de 15 - Objetivos y alcance

El objetivo principal es disponer de una capa gateway interna que permita publicar servicios backend de forma controlada, uniforme y observable. La pasarela debe permitir que n8n y otros consumidores invoquen endpoints JSON simplificados, mientras el gateway se encarga de localizar el backend real, aplicar reglas tecnicas y registrar la actividad.

Los objetivos funcionales de la solucion son:

- Exponer endpoints HTTP/JSON bajo rutas corporativas controladas.
- Encapsular servicios TIBCO, .NET u otros backends REST internos.
- Permitir configuracion por entorno y por servicio.
- Gestionar servicios activos e inactivos sin cambios de codigo.
- Restringir metodos HTTP publicados por servicio.
- Aplicar autorizacion por consumidor, servicio y metodo.
- Aplicar rate limit por consumidor o clave tecnica.
- Registrar auditoria de cada invocacion relevante.
- Generar metricas tecnicas para explotacion operacional.
- Normalizar respuestas de error para consumidores.

El alcance tecnico inicial incluye una aplicacion Spring Boot con rutas Apache Camel, proxy Undertow, invocacion HTTP dinamica, configuracion YAML, conexion JDBC a PostgreSQL, endpoints Actuator, metricas Prometheus y scripts de ejecucion y pruebas locales.

Quedan dentro del alcance evolutivo, pero no necesariamente completados en el primer bloque, las siguientes capacidades: validacion estricta de contratos JSON, mapeos complejos entre contratos de proxy y backend, mascarado real de campos sensibles, gestion avanzada de credenciales, despliegue en plataforma productiva, dashboards operacionales, alarmas y pipeline CI/CD completo.

La solucion no pretende implementar logica de negocio de cliente, producto, pedido o stock. Esa responsabilidad permanece en los sistemas propietarios. La pasarela solo aplica logica transversal: seguridad, trazabilidad, enrutamiento, transformacion tecnica, resiliencia y observabilidad.

El alcance tambien contempla la migracion del runtime objetivo a Java 21, manteniendo Spring Boot 3.5.14 y Apache Camel 4.14.7, ya compatibles con dicha version. Esta actualizacion aporta alineamiento con LTS moderna sin forzar cambios innecesarios en el framework.

<div style="page-break-after: always;"></div>

## Pagina 3 de 15 - Arquitectura general

La arquitectura propuesta sigue un patron de gateway interno. Los consumidores invocan el gateway a traves de rutas HTTP publicadas en el puerto de proxy. El gateway identifica la ruta, resuelve la configuracion efectiva, aplica controles transversales y reenvia la peticion al backend configurado. La respuesta del backend vuelve por el mismo canal, donde puede ser transformada, mascarada, auditada y medida.

La aplicacion tiene dos superficies HTTP diferenciadas. La primera es la superficie de negocio o proxy, expuesta mediante Apache Camel Undertow. En la configuracion actual se define el host `0.0.0.0` y el puerto `8082`. La segunda es la superficie de gestion Spring Boot Actuator, configurada en el puerto `8081`, con endpoints de salud, informacion, metricas y Prometheus.

Los principales bloques de arquitectura son:

- **Entrada HTTP:** rutas Undertow creadas dinamicamente desde el catalogo de servicios.
- **Orquestacion Camel:** pipeline de procesadores que aplica controles en orden.
- **Catalogo de servicios:** carga de YAML por entorno desde `config/environments/{environment}/services`.
- **Seguridad:** autenticacion tecnica preparada y autorizacion basada en permisos PostgreSQL.
- **Control de consumo:** rate limit por consumidor mediante throttling de Camel.
- **Backend invocation:** llamada dinamica a endpoint HTTP backend mediante `toD`.
- **Observabilidad:** metricas Micrometer, Prometheus, Actuator y auditoria SQL.
- **Errores:** normalizacion de excepciones tecnicas y respuestas de limite/autorizacion.

El flujo logico de una peticion es el siguiente:

1. El consumidor llama a una ruta publicada, por ejemplo `/api/v1/clientes/demo`.
2. Camel selecciona la ruta generada para el servicio activo y metodo HTTP.
3. Se inicializa la auditoria y se arrancan metricas de duracion si estan habilitadas.
4. Se asegura un identificador de correlacion.
5. Se valida que la ruta tiene contexto suficiente.
6. Se carga o prepara la configuracion efectiva.
7. Se ejecutan autenticacion y autorizacion.
8. Se aplica rate limit.
9. Se valida contrato y se mapea la peticion si procede.
10. Se invoca el backend configurado.
11. Se mapea la respuesta, se enmascara informacion sensible y se devuelve al consumidor.
12. En cierre de intercambio se registran metricas y auditoria.

Esta arquitectura permite que las capacidades transversales se apliquen de forma homogenea a todos los servicios publicados, evitando duplicidad y reduciendo riesgos de implementaciones inconsistentes.

<div style="page-break-after: always;"></div>

## Pagina 4 de 15 - Stack tecnologico

La solucion se basa en tecnologias consolidadas dentro del ecosistema Java empresarial. La seleccion prioriza mantenibilidad, compatibilidad con integraciones HTTP, facilidad de observabilidad y bajo coste operativo.

El nucleo de ejecucion es **Spring Boot 3.5.14**, que proporciona arranque, configuracion, gestion de propiedades, Actuator, integracion JDBC, empaquetado y convenciones operativas. Spring Boot reduce el esfuerzo de infraestructura de aplicacion y facilita la integracion con herramientas habituales de despliegue y monitorizacion.

El motor de integracion es **Apache Camel 4.14.7**. Camel es adecuado para este caso porque ofrece un DSL robusto de rutas, componentes HTTP, Undertow, SQL, Micrometer, gestion de excepciones, throttling, procesadores y patrones de integracion empresarial. La pasarela se beneficia de Camel al representar cada servicio como una ruta declarativa y extensible.

Para la exposicion HTTP del proxy se utiliza **camel-undertow**, que permite levantar endpoints HTTP directamente desde Camel. Para llamadas salientes a servicios internos se utiliza **camel-http**. Para serializacion y lectura de configuraciones YAML se emplean Jackson y `jackson-dataformat-yaml`.

La observabilidad se implementa con **Micrometer**, **Prometheus Registry** y **Spring Boot Actuator**. Esto permite exponer metricas tecnicas sin acoplar la aplicacion a una herramienta concreta de monitorizacion. Prometheus puede recolectar los datos, y Grafana u otra plataforma puede visualizarlos.

La persistencia operacional se apoya en **PostgreSQL** a traves de JDBC y el componente SQL de Camel. La base de datos se utiliza para autorizacion de consumidores y auditoria de llamadas. Esta separacion permite gestionar permisos sin recompilar la pasarela y conservar evidencias de uso.

El build se gestiona con **Maven 3.9.x**. El proyecto actualmente compila con Java 17, pero el plan tecnico contempla actualizar la propiedad `java.version` a Java 21, manteniendo las versiones actuales de Spring Boot, Camel y plugins, ya que son compatibles con Java 21.

Dependencias principales:

- `spring-boot-starter-actuator`
- `camel-spring-boot-starter`
- `camel-undertow-starter`
- `camel-http-starter`
- `camel-jackson-starter`
- `camel-micrometer-starter`
- `camel-sql-starter`
- `micrometer-registry-prometheus`
- `spring-boot-starter-jdbc`
- `postgresql`

La seleccion tecnologica evita componentes innecesarios y permite evolucionar hacia contenedores, Kubernetes, OpenShift u otra plataforma corporativa sin redisenar el nucleo.

<div style="page-break-after: always;"></div>

## Pagina 5 de 15 - Modelo de configuracion y catalogo de servicios

Uno de los elementos clave de la solucion es el catalogo de servicios configurado por ficheros YAML. Este enfoque permite separar la publicacion de endpoints del codigo Java. La aplicacion carga en arranque los ficheros ubicados bajo el entorno activo, por ejemplo `config/environments/local/services/*.yml`.

La propiedad `gateway.environment` determina el entorno de configuracion. En local se utiliza `local`, por lo que el cargador busca definiciones en `config/environments/local/services`. Este patron se puede extender a `dev`, `pre`, `uat`, `prod` u otros entornos, manteniendo definiciones separadas y controlables por versionado.

Cada fichero YAML contiene una definicion de servicio con la siguiente informacion:

- `id`: identificador tecnico unico del servicio.
- `name`: nombre legible para operaciones y gobierno.
- `status`: estado del servicio, por ejemplo `active`.
- `exposure.base_path`: ruta publicada por el gateway.
- `exposure.methods`: metodos HTTP permitidos.
- `backend.type`: tipo de backend, inicialmente `rest`.
- `backend.method`: metodo HTTP usado contra el backend.
- `backend.endpoint_url`: URL tecnica del backend real.
- `rate_limit`: politica especifica de limitacion.
- `metrics`: activacion o desactivacion de metricas.
- `audit`: activacion o desactivacion de auditoria.

Ejemplo actual:

```yaml
service:
  id: clientes-consulta-v1
  name: Consulta de cliente demo
  status: active
  exposure:
    base_path: /api/v1/clientes/demo
    methods: [GET]
  backend:
    type: rest
    method: GET
    endpoint_url: http://localhost:9090/clientes/demo?bridgeEndpoint=true
  rate_limit:
    enabled: true
    requests: 60
    window_seconds: 60
  metrics:
    enabled: true
  audit:
    enabled: true
```

Durante el arranque, `ServiceCatalogLoader` lee los recursos YAML con Jackson, construye objetos `ServiceDefinition` y los registra en un `ServiceCatalog` inmutable. `ProxyRouteBuilder` recorre este catalogo y crea una ruta Camel por cada servicio activo y metodo expuesto.

Este modelo aporta ventajas claras para la oferta:

- Alta de nuevos servicios con cambios de configuracion.
- Control de version de definiciones por Git.
- Separacion entre comportamiento transversal y endpoints concretos.
- Posibilidad de activar/desactivar servicios por entorno.
- Politicas por defecto globales y overrides por servicio.

Como evolucion recomendada, se puede anadir validacion de esquema YAML en arranque para detectar configuraciones incompletas antes de publicar rutas.

<div style="page-break-after: always;"></div>

## Pagina 6 de 15 - Flujo de procesamiento

El flujo de procesamiento esta implementado como una ruta Camel compuesta por procesadores especializados. Cada procesador tiene una responsabilidad acotada, lo que facilita pruebas, evolucion y mantenimiento.

La secuencia prevista por el README y materializada en la ruta principal es:

1. `correlationIdProcessor`
2. `routeResolverProcessor`
3. `effectiveConfigLoaderProcessor`
4. `authProcessor`
5. `authorizationProcessor`
6. `rateLimitProcessor`
7. `contractValidationProcessor`
8. `requestMappingProcessor`
9. invocacion al backend
10. `responseMappingProcessor`
11. `maskingProcessor`
12. auditoria y metricas en cierre

Antes del flujo principal, la ruta establece propiedades de intercambio como `serviceId`, `backendType`, `backendEndpoint`, `requestPath`, `metricsEnabled` y `auditEnabled`. Estas propiedades funcionan como contexto interno de la peticion.

La auditoria se inicia al principio mediante `auditProcessor.start`. En ese momento se capturan datos que deben conservarse aunque la peticion falle: consumidor, metodo, path, IP cliente, user agent, tamano de request y timestamp de inicio. Al finalizar el intercambio, `auditProcessor.prepare` completa respuesta, duracion, codigo de error, autorizacion, rate limit, endpoint backend y tamano de respuesta.

Las metricas se arrancan al inicio si estan habilitadas. En el cierre se incrementa el contador `gateway.proxy.requests` y se detiene el timer `gateway.proxy.duration`. Las etiquetas incluyen servicio, metodo y estado de respuesta. Esto permite visualizar volumen, latencia y errores por servicio.

La autorizacion se resuelve en dos pasos. Primero, `authorizationProcessor.prepare` valida que exista consumidor y servicio, y prepara los parametros SQL. Despues Camel ejecuta `authorize-consumer.sql` contra PostgreSQL. Finalmente, `authorizationProcessor.enforce` corta la ruta con `403` si el consumidor no esta autorizado.

El rate limit utiliza `RateLimitProcessor` para resolver una clave de consumidor. La ruta aplica despues `throttle` de Camel con ventana temporal y rechazo inmediato. Si se supera el limite, se devuelve `429` con error normalizado.

La llamada al backend se hace con `toD("${exchangeProperty.backendEndpoint}")`, permitiendo que el endpoint de destino sea dinamico por servicio. Antes de llamar se eliminan cabeceras HTTP de entrada que podrian interferir con la URL backend.

El diseno del flujo permite incorporar nuevas capacidades sin romper el contrato externo. Por ejemplo, la validacion de contrato y el mascarado ya tienen procesadores reservados, aunque su implementacion puede evolucionar por fases.

<div style="page-break-after: always;"></div>

## Pagina 7 de 15 - Seguridad y autorizacion

La seguridad de la solucion se estructura en varias capas. La primera capa es la identificacion del consumidor. Actualmente el gateway espera una cabecera `X-Consumer-Id` para identificar al llamante en los controles de autorizacion y auditoria. Como alternativa para rate limit, puede usar `X-Api-Key`, `X-Forwarded-For`, `X-Real-IP` o la direccion remota Camel.

La segunda capa es la autorizacion por servicio y metodo. La pasarela no concede acceso por el simple hecho de conocer una URL. Para cada peticion autorizable, el gateway consulta PostgreSQL y verifica si el consumidor tiene permiso directo sobre el servicio o pertenece a un grupo con permiso activo.

La consulta SQL `authorize-consumer.sql` contempla dos modelos:

- Permisos directos en `gateway_ops.consumer_service_permissions`.
- Permisos heredados por grupo a traves de `consumer_group_members`, `consumer_groups` y `group_service_permissions`.

En ambos casos se comprueba que el permiso este activo, que el servicio coincida y que el metodo HTTP este permitido. El campo `allowed_methods` acepta `*` o una lista de metodos normalizados en mayusculas.

Si falta identidad de consumidor, la pasarela responde `401 unauthorized`. Si existe consumidor pero no tiene permiso, responde `403 forbidden`. Estas respuestas se devuelven en JSON con estructura uniforme, evitando que cada backend tenga que resolver el mismo problema.

La propiedad `gateway.authorization.enabled` permite activar o desactivar globalmente la autorizacion. Esto es util para entornos locales o fases iniciales de pruebas, pero en entornos productivos debe permanecer activada.

Para una implantacion productiva se recomienda completar las siguientes capacidades:

- Definir el mecanismo formal de autenticacion: API key, JWT corporativo, mTLS o integracion con plataforma IAM.
- Evitar que `X-Consumer-Id` sea confiado sin validacion criptografica o perimetral.
- Gestionar secretos mediante vault o servicio corporativo, no en ficheros planos.
- Restringir redes de entrada y salida a traves de firewall, security groups o service mesh.
- Incorporar trazabilidad de decisiones de autorizacion en auditoria.
- Definir procedimientos de alta, baja y caducidad de consumidores.

El diseno actual deja el punto de autenticacion encapsulado en `AuthProcessor`, lo que permite introducir el mecanismo definitivo sin redisenar la ruta principal.

<div style="page-break-after: always;"></div>

## Pagina 8 de 15 - Control de consumo y resiliencia

El gateway incorpora limitacion de peticiones para proteger backends y evitar usos abusivos o accidentales. La politica global se define en `application.yml` mediante:

```yaml
gateway:
  rate-limit:
    enabled: true
    requests: 60
    window-seconds: 60
```

Cada servicio puede heredar esta politica o definir su propio bloque `rate_limit`. La resolucion efectiva prioriza la configuracion especifica de servicio y, si falta, utiliza los valores globales. Este modelo permite aplicar limites mas estrictos a servicios sensibles y limites mas amplios a servicios de bajo riesgo.

La clave de rate limit se calcula en `RateLimitProcessor`. El orden de resolucion es:

1. `X-Consumer-Id`
2. `X-Api-Key`
3. `X-Forwarded-For`
4. `X-Real-IP`
5. `CamelHttpRemoteAddress`
6. `anonymous`

La ruta Camel aplica `throttle` con `rejectExecution(true)`, por lo que las peticiones que superan el umbral no quedan esperando: se rechazan con `429 Too Many Requests`. Esta decision es adecuada para una pasarela que debe proteger recursos internos y mantener latencias predecibles.

La resiliencia actual se centra en control de caudal y normalizacion de errores. Cualquier excepcion no controlada se captura con `onException(Exception.class)` y se transforma en un error JSON con codigo HTTP `502`. Las excepciones de rate limit tienen tratamiento especifico y devuelven `429`.

Como evolucion para entornos productivos se recomienda incorporar:

- Timeouts explicitos de conexion y lectura para backends.
- Circuit breaker por servicio para evitar cascadas de fallo.
- Retries controlados solo para operaciones idempotentes.
- Bulkheads o aislamiento por familia de servicios.
- Politicas diferenciadas para TIBCO, .NET y otros proveedores.
- Cabeceras de diagnostico no sensibles para soporte.
- Health checks funcionales por dependencia critica.

La solucion debe evitar retries indiscriminados en operaciones no idempotentes, ya que podrian duplicar acciones de negocio. Por ello, las politicas de resiliencia deben ser configurables por servicio y metodo.

<div style="page-break-after: always;"></div>

## Pagina 9 de 15 - Auditoria y trazabilidad

La auditoria es una capacidad central de la solucion. Cada intercambio puede generar un registro en PostgreSQL con datos suficientes para reconstruir quien llamo, que servicio se uso, cuando ocurrio, cuanto tardo, cual fue la respuesta y si hubo error, autorizacion denegada o rate limit.

La tabla objetivo esperada esta bajo el esquema `gateway_ops`, concretamente `gateway_ops.audit_events`. La insercion se realiza mediante el SQL `insert-audit-event.sql`, invocado desde Camel en el bloque `onCompletion`. Esto asegura que la auditoria se ejecute al cierre de la peticion, tanto en exito como en error controlado, siempre que la auditoria este habilitada.

Los campos contemplados incluyen:

- `correlation_id`
- `consumer_id`
- `service_id`
- `http_method`
- `request_path`
- `response_code`
- `elapsed_ms`
- `error_code`
- `authorized`
- `rate_limited`
- `client_ip`
- `user_agent`
- `backend_endpoint`
- `request_size`
- `response_size`
- `created_at`

El identificador de correlacion se gestiona con `CorrelationIdProcessor`. Si el consumidor envia `X-Correlation-Id`, se conserva. Si no lo envia, el gateway genera un UUID. Este identificador se propaga como cabecera y se utiliza en auditoria, facilitando el seguimiento extremo a extremo.

La resolucion de IP cliente considera `X-Forwarded-For`, `X-Real-IP` y la direccion remota de Camel. En arquitecturas con balanceador o proxy frontal, sera necesario asegurar que dichas cabeceras se gestionan correctamente y que no pueden ser falsificadas desde redes no confiables.

La auditoria es configurable. Existe un valor global `gateway.audit.enabled` y un override por servicio. Para servicios de alto volumen y bajo riesgo se podria desactivar o reducir auditoria, aunque la recomendacion para integraciones empresariales es mantener al menos auditoria tecnica basica.

En una fase posterior se recomienda:

- Definir retencion de auditoria por politica corporativa.
- Indexar campos de busqueda frecuentes: fecha, consumidor, servicio, correlacion y codigo de respuesta.
- Separar datos tecnicos de datos personales.
- Aplicar mascarado antes de persistir valores sensibles.
- Incorporar dashboards y consultas operacionales.
- Definir procedimiento de exportacion para soporte e incidencias.

<div style="page-break-after: always;"></div>

## Pagina 10 de 15 - Observabilidad y operacion

La pasarela expone informacion operacional a traves de Spring Boot Actuator y Micrometer. En la configuracion actual, los endpoints de gestion publicados son `health`, `info`, `metrics` y `prometheus`. El puerto de gestion coincide con el puerto Spring Boot `8081`, mientras la superficie de proxy se publica en `8082`.

Los endpoints relevantes son:

- `/actuator/health`: estado de salud.
- `/actuator/info`: informacion de aplicacion.
- `/actuator/metrics`: catalogo y consulta de metricas.
- `/actuator/prometheus`: formato scrapeable por Prometheus.

Camel Micrometer esta configurado para habilitar politicas de ruta y notificadores de eventos. Ademas, la ruta principal genera metricas explicitas:

- `gateway.proxy.requests`: contador de peticiones procesadas.
- `gateway.proxy.duration`: duracion de peticiones.

Las etiquetas asociadas permiten segmentar por:

- `serviceId`
- `method`
- `status`

Esto habilita indicadores basicos de explotacion:

- Volumen total por servicio.
- Volumen por metodo HTTP.
- Distribucion de codigos de respuesta.
- Latencia por ruta.
- Errores por servicio.
- Impacto de rate limit.

La solucion debe integrarse con la plataforma de monitorizacion corporativa. En un escenario Prometheus/Grafana, Prometheus recolectaria `/actuator/prometheus` y Grafana mostraria dashboards de disponibilidad, latencia, errores y trafico. En otras plataformas, Micrometer puede adaptarse con registros alternativos.

Para operacion productiva se recomienda definir alertas como:

- Error rate por encima de umbral.
- Latencia p95 o p99 por encima de objetivo.
- Aumento de respuestas `429`.
- Respuestas `401` o `403` anormalmente altas.
- Fallos de conexion a PostgreSQL.
- Fallos de conexion a backends criticos.
- Ausencia de trafico en servicios esperados.

Los logs de aplicacion deben incluir correlacion, servicio y resultado. La configuracion final deberia emitir logs estructurados JSON si la plataforma de observabilidad lo requiere. No se recomienda registrar cuerpos completos de peticiones o respuestas en logs generales, especialmente si pueden contener datos personales o sensibles.

<div style="page-break-after: always;"></div>

## Pagina 11 de 15 - Contratos, mapeos y datos sensibles

La solucion esta preparada para separar el contrato expuesto por el gateway del contrato interno del backend. Esta separacion es importante porque permite ofrecer a n8n y otros consumidores una API estable, aunque los sistemas TIBCO o .NET evolucionen de forma independiente.

En el estado actual existen procesadores reservados:

- `ContractValidationProcessor`
- `RequestMappingProcessor`
- `ResponseMappingProcessor`
- `MaskingProcessor`

`RequestMappingProcessor` ya elimina cabeceras HTTP como `Exchange.HTTP_URI`, `Exchange.HTTP_PATH` y `Exchange.HTTP_QUERY` antes de invocar el backend. Esto evita que detalles de la peticion original interfieran con la URL destino. Los otros procesadores estan preparados como puntos de extension.

La validacion de contratos deberia cubrir:

- Metodo HTTP permitido.
- Presencia y formato de cabeceras obligatorias.
- Parametros de query permitidos.
- Estructura JSON de entrada.
- Tipos y formatos de campos.
- Reglas basicas de obligatoriedad.
- Tamano maximo de payload.

Para contratos JSON se recomienda utilizar JSON Schema o una especificacion OpenAPI como fuente de verdad. La validacion debe ejecutarse antes de invocar el backend, devolviendo errores `400` claros cuando la peticion sea invalida.

Los mapeos de request y response deben permitir:

- Renombrar campos.
- Cambiar estructuras JSON.
- Adaptar codigos o enumerados.
- Construir parametros backend desde campos de entrada.
- Filtrar campos no expuestos.
- Normalizar respuestas de diferentes proveedores.

La implementacion puede evolucionar desde transformaciones Java especificas hacia reglas declarativas por servicio si el volumen de integraciones lo justifica. Para una primera fase, mantener transformaciones Java controladas puede ser mas seguro y facil de probar.

El mascarado de datos sensibles debe aplicarse antes de devolver respuestas, auditar o registrar informacion. Campos como identificadores personales, email, telefono, documentos, direcciones, tokens o datos de pago deben tratarse segun politicas corporativas. El `MaskingProcessor` es el punto tecnico previsto para esta responsabilidad.

La recomendacion para la oferta es incluir un catalogo inicial de campos sensibles, reglas de mascarado y pruebas automatizadas que garanticen que dichos campos no aparecen en claro en salidas no autorizadas.

<div style="page-break-after: always;"></div>

## Pagina 12 de 15 - Persistencia operacional y modelo de datos

La base de datos PostgreSQL cumple una funcion operacional, no transaccional de negocio. Su objetivo es soportar autorizacion y auditoria de la pasarela. La aplicacion se conecta mediante `spring.datasource`, con valores parametrizables por variables de entorno:

```yaml
spring:
  datasource:
    url: ${GATEWAY_DB_URL:jdbc:postgresql://192.168.0.13:5432/camel_gateway_ops}
    username: ${GATEWAY_DB_USERNAME:desigual}
    password: ${GATEWAY_DB_PASSWORD:desigual}
```

La autorizacion requiere tablas de permisos directos y permisos por grupo. Aunque el DDL no forma parte visible del proyecto actual, las consultas indican el modelo esperado:

- `gateway_ops.consumer_service_permissions`
- `gateway_ops.consumer_group_members`
- `gateway_ops.consumer_groups`
- `gateway_ops.group_service_permissions`
- `gateway_ops.audit_events`

El modelo permite que un consumidor tenga permisos directos sobre un servicio o que los herede por pertenencia a grupo. Esta aproximacion facilita administrar permisos por perfiles de consumo en lugar de mantener reglas repetidas para cada consumidor.

La tabla de auditoria debe disenarse para soportar volumen y consultas. Se recomienda:

- Clave primaria tecnica.
- Indice por `created_at`.
- Indice por `correlation_id`.
- Indice compuesto por `service_id`, `created_at`.
- Indice compuesto por `consumer_id`, `created_at`.
- Particionado temporal si el volumen crece.
- Politica de retencion y purgado.

La disponibilidad de PostgreSQL afecta a dos capacidades. Si la base de datos no esta disponible, la autorizacion no puede decidir permisos y la auditoria no puede persistir eventos. La politica recomendada para produccion es fail closed en autorizacion: si no se puede validar permiso, se deniega o se devuelve error tecnico. Para auditoria, la politica puede ser fail fast o cola asincrona, segun criticidad.

En fases posteriores se puede desacoplar auditoria mediante mensajeria o buffer interno, pero para una primera entrega JDBC directo es simple, comprensible y suficiente si el volumen es moderado y la base esta correctamente dimensionada.

Las credenciales de base de datos no deben quedar fijadas en configuraciones productivas. Deben inyectarse desde variables de entorno, secretos de plataforma o vault corporativo.

<div style="page-break-after: always;"></div>

## Pagina 13 de 15 - Despliegue, entornos y migracion a Java 21

La aplicacion se empaqueta como artefacto Maven ejecutable de Spring Boot. Puede desplegarse como proceso Java tradicional, servicio systemd, contenedor Docker o workload en plataforma Kubernetes/OpenShift. La solucion no depende de una topologia concreta, pero se recomienda contenedorizacion para entornos gestionados.

La configuracion por entorno se basa en:

- Propiedades Spring (`application.yml` y variables de entorno).
- `gateway.environment` para seleccionar catalogo de servicios.
- Ficheros YAML por entorno bajo `config/environments`.
- Variables para credenciales y URL de PostgreSQL.
- Parametros de plataforma para puertos, memoria y health checks.

Los puertos actuales son:

- `8081`: aplicacion Spring Boot y Actuator.
- `8082`: proxy HTTP Camel.
- `9090`: backend mock local para pruebas.

En despliegue productivo se recomienda publicar solo la superficie necesaria. El puerto de Actuator debe protegerse por red, autenticacion o mecanismo de plataforma. El puerto de proxy debe exponerse a los consumidores autorizados, preferiblemente detras de balanceador, API manager corporativo o ingress controlado.

El proyecto dispone de un plan de actualizacion a Java 21. El plan detecta que Spring Boot 3.5.14 y Maven 3.9.11 son compatibles, por lo que el cambio principal es actualizar `java.version` de `17` a `21` en `pom.xml` y validar compilacion y tests con JDK 21.

Pasos recomendados de migracion:

1. Verificar Maven 3.9.x y JDK 21 disponibles.
2. Ejecutar baseline con Java 17.
3. Cambiar `java.version` a `21`.
4. Compilar y ejecutar tests con Java 21.
5. Validar arranque local.
6. Ejecutar pruebas de proxy, autorizacion, rate limit, auditoria y metricas.
7. Actualizar imagen base o runtime de despliegue.
8. Promocionar a entornos superiores.

Esta migracion debe tratarse como cambio tecnico controlado. Aunque el analisis estatico no identifica incompatibilidades de codigo, Java 21 puede introducir diferencias de runtime. Por ello, la validacion funcional y operacional es obligatoria antes de produccion.

<div style="page-break-after: always;"></div>

## Pagina 14 de 15 - Plan de implantacion

La implantacion se propone por fases para reducir riesgo y entregar valor de forma progresiva.

**Fase 1 - Fundacion tecnica**

Se consolida el proyecto base, build Maven, runtime Java 21, configuracion Spring Boot, arranque local, Actuator y estructura de paquetes. Se valida que el servicio arranca de forma reproducible y que los scripts de build y ejecucion funcionan.

**Fase 2 - Catalogo y rutas proxy**

Se completa el modelo de catalogo YAML por entorno y la generacion de rutas Camel por servicio activo. Se documenta el formato de definicion de servicios y se publica un primer servicio demo contra backend mock. Se validan metodos permitidos, endpoint dinamico y respuestas.

**Fase 3 - Seguridad operacional**

Se activa el modelo de autorizacion con PostgreSQL, alta de consumidores, permisos directos y permisos por grupo. Se define el mecanismo de identidad definitivo para consumidores y se endurece el tratamiento de cabeceras. Se establecen respuestas `401` y `403` normalizadas.

**Fase 4 - Observabilidad y auditoria**

Se completa la auditoria en `gateway_ops.audit_events`, indices, retencion inicial y consultas operacionales. Se integran metricas Prometheus y se crean dashboards basicos de trafico, latencia y errores. Se validan health checks y readiness/liveness si aplica plataforma contenedorizada.

**Fase 5 - Contratos y transformaciones**

Se implementa validacion JSON/OpenAPI para servicios seleccionados, mapeos de request/response y mascarado de datos sensibles. Se priorizan los servicios con mayor exposicion o criticidad.

**Fase 6 - Hardening y produccion**

Se anaden timeouts, circuit breakers, gestion de secretos, configuracion productiva, pipeline CI/CD, pruebas automatizadas y procedimientos de operacion. Se ejecutan pruebas de carga basicas y se ajustan limites por servicio.

Entregables principales:

- Codigo fuente de la pasarela.
- Documentacion de configuracion de servicios.
- Scripts o pipeline de build.
- SQL operativo necesario.
- Documento de despliegue.
- Dashboard o definicion de metricas.
- Plan de pruebas.
- Guia de operacion e incidencias.

Este enfoque permite arrancar con un producto minimo solido y ampliar capacidades segun prioridad de negocio.

<div style="page-break-after: always;"></div>

## Pagina 15 de 15 - Riesgos, supuestos y cierre

La solucion propuesta es tecnicamente viable y se apoya en un stack adecuado para integraciones internas. No obstante, hay decisiones que deben cerrarse durante la implantacion para asegurar que el gateway pueda operar de forma segura y estable.

Supuestos principales:

- Los consumidores pueden enviar una identidad tecnica fiable o se definira un mecanismo formal de autenticacion.
- Los backends TIBCO/.NET son accesibles desde la red donde se despliegue el gateway.
- PostgreSQL estara disponible para permisos y auditoria.
- La organizacion dispone de plataforma de monitorizacion compatible con Prometheus o Actuator.
- Las definiciones YAML se versionaran y pasaran por revision antes de desplegarse.
- Los equipos propietarios de backend proporcionaran contratos, ejemplos y criterios de error.

Riesgos principales y mitigaciones:

- **Identidad no verificada:** usar `X-Consumer-Id` sin autenticacion fuerte no es suficiente en produccion. Mitigacion: API key, JWT, mTLS o validacion en perimetro.
- **Dependencia de PostgreSQL:** una caida puede afectar autorizacion y auditoria. Mitigacion: alta disponibilidad, timeouts, monitorizacion y politica fail closed.
- **Backends lentos:** pueden agotar recursos del gateway. Mitigacion: timeouts, circuit breakers y limites por servicio.
- **Configuraciones invalidas:** YAML incorrecto puede impedir publicar rutas. Mitigacion: validacion de esquema y pruebas de configuracion en CI.
- **Datos sensibles:** logs o auditoria pueden exponer informacion. Mitigacion: mascarado, minimizacion de datos y revision de campos.
- **Rate limit en memoria:** si hay varias replicas, el throttling local no comparte estado. Mitigacion: definir si el limite debe ser por replica o centralizarlo en Redis/API manager si se requiere limite global.

La conclusion tecnica es que Camel Gateway proporciona una base adecuada para una capa proxy interna entre n8n y sistemas corporativos. La estructura actual ya contiene los puntos de extension necesarios para evolucionar hacia un gateway gobernado: catalogo por configuracion, autorizacion SQL, auditoria, metricas, rate limit, normalizacion de errores y pipeline de procesadores.

Para la oferta se recomienda posicionar la solucion como una implantacion incremental: primero asegurar el nucleo operativo y los controles transversales, despues incorporar contratos, mapeos y hardening productivo. Este enfoque reduce el riesgo inicial y crea una plataforma reutilizable para nuevas integraciones.

---

## Anexo - Referencias del repositorio

- Aplicacion principal: `src/main/java/com/desigual/camelgateway/CamelGatewayApplication.java`
- Ruta proxy: `src/main/java/com/desigual/camelgateway/routes/ProxyRouteBuilder.java`
- Catalogo de servicios: `src/main/java/com/desigual/camelgateway/service/ServiceCatalogLoader.java`
- Configuracion global: `src/main/resources/application.yml`
- Servicio demo: `src/main/resources/config/environments/local/services/clientes-consulta-v1.yml`
- Autorizacion SQL: `src/main/resources/sql/authorize-consumer.sql`
- Auditoria SQL: `src/main/resources/sql/insert-audit-event.sql`
- Plan Java 21: `.github/java-upgrade/20260518054506/plan.md`
