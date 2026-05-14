#!/bin/bash

curl -sS http://localhost:8081/actuator/info |jq

exit 0;

