#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
#java -Dcantaloupe.config=$SCRIPT_DIR/cantaloupe.properties -Xmx2g -jar $SCRIPT_DIR/cantaloupe.war
java -cp $SCRIPT_DIR/cantaloupe.jar -Dcantaloupe.config=$SCRIPT_DIR/cantaloupe.properties edu.illinois.library.cantaloupe.StandaloneEntry