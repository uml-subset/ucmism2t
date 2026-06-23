#!/bin/bash

# UCMISM2T_DIR       - UCMISM2T folder
# MODEL_DIR          - folder with model XMI file
# GENERIC_PROPERTIES - file with generic properties
# MODEL_PROPERTIES   - file with model-specific properties

export UCMISM2T_DIR=/media/wackerow/Shared/Git/github/ucmism2t
export MODEL_FILE=${UCMISM2T_DIR}/model/ddi-cdi_canonical-unique-names-eclipse.xmi
export GENERIC_PROPERTIES=${UCMISM2T_DIR}/property/generic.properties
export MODEL_PROPERTIES="${MODEL_FILE%.*}.properties"
#export MODEL_PROPERTIES=$("${MODEL_FILE%.*}").properties
#export MODEL_PROPERTIES=$(basename "${MODEL_FILE%.*}").properties
export LAUNCHER=${UCMISM2T_DIR}/releng/ucmism2t.product/target/products/ucmism2t.product/linux/gtk/x86_64/ucmism2t

# Software
export SOFTWARE_HOME=/home/wackerow/software
export SAXONJAR=${SOFTWARE_HOME}/saxon/SaxonHE12-0J/saxon-he-12.0.jar
export PLANTUMLJAR=${SOFTWARE_HOME}/plantuml/plantuml-1.2025.2.jar
