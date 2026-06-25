#!/bin/bash

source ./set_env.sh

# runs 6 minutes on a laptop.

ant -Dbasedir=${WORKING_FOLDER} -buildfile ${UCMISM2T_DIR}/cli/task/DDI-CDI_Sphinx.xml