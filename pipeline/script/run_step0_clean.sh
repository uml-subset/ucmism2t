#!/bin/sh

ant -buildfile ../ant/step0_clean.xml \
    -DDpipeline.properties=pipeline.properties \
    -Dbasedir=.
