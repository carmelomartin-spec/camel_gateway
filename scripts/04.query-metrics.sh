#!/bin/bash

curl -sS http://localhost:8081/actuator/metrics | jq

exit 0;

