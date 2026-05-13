#!/bin/bash

curl -sS \
  -H "X-Consumer-Id: n8n-demo" \
  http://localhost:8082/api/v1/clientes/demo | jq

exit 0;

