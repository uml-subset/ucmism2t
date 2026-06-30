#!/bin/sh

ucmism2t_dir=$(sed -n 's/^ucmism2t_dir *= *//p' pipeline.properties)

ant -buildfile ${ucmism2t_dir}/pipeline/ant/all_steps.xml \
    -propertyfile pipeline.properties
