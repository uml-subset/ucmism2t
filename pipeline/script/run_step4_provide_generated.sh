#!/bin/sh

ant -buildfile ../ant/step4_provide_generated.xml \
    -DDpipeline.properties=pipeline.properties \
    -Dbasedir=.
