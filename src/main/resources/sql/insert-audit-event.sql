insert into gateway_ops.audit_events (
    correlation_id,
    consumer_id,
    service_id,
    http_method,
    request_path,
    response_code,
    elapsed_ms,
    error_code,
    client_ip,
    user_agent,
    backend_endpoint,
    created_at
) values (
    :#AuditCorrelationId,
    :#AuditConsumerId,
    :#AuditServiceId,
    :#AuditMethod,
    :#AuditPath,
    :#AuditResponseCode,
    :#AuditElapsedMs,
    :#AuditErrorCode,
    :#AuditClientIp,
    :#AuditUserAgent,
    :#AuditBackendEndpoint,
    current_timestamp
)
