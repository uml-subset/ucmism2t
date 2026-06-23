#!/bin/bash

# build the binary

source ./set_env.sh

( cd ${UCMISM2T_DIR} ; mvn clean verify -Dbuild.platform=all )
# ( cd ${UCMISM2T_DIR} ; mvn clean verify -Dbuild.platform=linux )
