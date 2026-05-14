#!/bin/bash

curl -sS http://localhost:8081/actuator/health | jq 

exit 0;

