select (
    exists (
        select 1
        from gateway_ops.consumer_service_permissions csp
        where csp.consumer_id = :#AuthorizationConsumerId
          and csp.service_id = :#AuthorizationServiceId
          and csp.status = 'active'
          and (
              csp.allowed_methods = '*'
              or :#AuthorizationMethod = any(string_to_array(upper(replace(csp.allowed_methods, ' ', '')), ','))
          )
    )
    or exists (
        select 1
        from gateway_ops.consumer_group_members cgm
        join gateway_ops.consumer_groups cg
          on cg.id = cgm.group_id
        join gateway_ops.group_service_permissions gsp
          on gsp.group_id = cgm.group_id
        where cgm.consumer_id = :#AuthorizationConsumerId
          and cg.status = 'active'
          and gsp.status = 'active'
          and gsp.service_id = :#AuthorizationServiceId
          and (
              gsp.allowed_methods = '*'
              or :#AuthorizationMethod = any(string_to_array(upper(replace(gsp.allowed_methods, ' ', '')), ','))
          )
    )
) as authorized
