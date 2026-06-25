#!/bin/bash

source ./set_env.sh

# see: https://www.saxonica.com/html/documentation12/using-xsl/xsltfromant.html
export CLASSPATH=${SAXON_JAR}

ant -Dbasedir=${WORKING_FOLDER} -buildfile ${UCMISM2T_DIR}/cli/task/step2_preSphinx.xml