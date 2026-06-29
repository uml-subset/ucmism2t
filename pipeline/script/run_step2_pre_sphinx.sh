#!/bin/sh

ucmism2t_dir=$(sed -n 's/^ucmism2t_dir *= *//p' pipeline.properties)

ant -buildfile ${ucmism2t_dir}/pipeline/ant/step2_pre_sphinx.xml \
    -propertyfile pipeline.properties
