#!/bin/bash

ucmism2t_dir=$(sed -n 's/^ucmism2t_dir *= *//p' pipeline.properties)

ant -buildfile ${ucmism2t_dir}/pipeline/ant/step0_clean.xml \
    -propertyfile pipeline.properties
