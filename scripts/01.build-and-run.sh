#!/usr/bin/env bash
set -euo pipefail

mvn clean test
mvn spring-boot:run
