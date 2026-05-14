#!/bin/bash

#Se puede hacer un grep por "gateway_proxy" o por "clientes-consulta-v1" o por "camel_" para mayor claridad 

curl -sS http://localhost:8081/actuator/prometheus | egrep -v "^#"

exit 0;

