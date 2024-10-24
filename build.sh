#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
mvn clean package -DskipTests
cp "$SCRIPT_DIR"/target/cantaloupe.jar "$SCRIPT_DIR"/cantaloupe.jar
