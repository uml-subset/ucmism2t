#!/bin/bash

# run the generation

# property values from first property file can be overwritten by the ones from the second file

source ./set_env.sh

# with property files in default locations
# Generic properties: property/generic.properties
# Model-specific properties (same basename as model filename): model/ddi-cdi_canonical-unique-names-eclipse.properties
# ./ucmism2t-wrapper.sh \
#      -input ./model/ddi-cdi_canonical-unique-names-eclipse.xmi \
#      -output ./generated

# property files with config argument
./ucmism2t-wrapper.sh         \
     -input ${MODEL_FILE}     \
     -output ./generated      \
     -config ${GENERIC_PROPERTIES},${MODEL_PROPERTIES}
