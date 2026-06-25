#!/bin/bash

# These environment variables must be set before running any shell scripts.

# UCMISM2T_DIR       - UCMISM2T folder
# MODEL_FILE         - model XMI file
# MODEL_FILE_ECLIPSE - model XMI file Eclipse variant
# GENERIC_PROPERTIES - file with generic properties
# MODEL_PROPERTIES   - file with model-specific properties
# LAUNCHER           - execution binary
# WORKING_FOLDER     - working folder
# GENERATED          - folder with generated artefacts
# SAXON_JAR          - Saxon jar
# PLANTML_JAR        - PlanUML Jar

export UCMISM2T_DIR=/home/wackerow/TEST/ucmism2t
#export UCMISM2T_DIR=/media/wackerow/Shared/Git/github/ucmism2t
export MODEL_FILE_ECLIPSE=${UCMISM2T_DIR}/model/ddi-cdi_canonical-unique-names-eclipse.xmi
export MODEL_FILE=${UCMISM2T_DIR}/model/ddi-cdi_canonical-unique-names.xmi
export GENERIC_PROPERTIES=${UCMISM2T_DIR}/property/generic.properties
export MODEL_PROPERTIES="${MODEL_FILE_ECLIPSE%.*}.properties"
export LAUNCHER=${UCMISM2T_DIR}/releng/ucmism2t.product/target/products/ucmism2t.product/linux/gtk/x86_64/ucmism2t
export WORKING_FOLDER=/home/wackerow/TEST/ucmism2t
#export WORKING_FOLDER=/home/wackerow/Downloads/x
export GENERATED_FOLDER=${WORKING_FOLDER}/generated
export LOG_FOLDER=${WORKING_FOLDER}/log

# Software
export SOFTWARE_HOME=/home/wackerow/software
export SAXON_JAR=${SOFTWARE_HOME}/saxon/SaxonHE12-9J/saxon-he-12.9.jar
export PLANTUML_JAR=${SOFTWARE_HOME}/plantuml/plantuml-asl-1.2026.6.jar
