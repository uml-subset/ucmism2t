#!/bin/sh

ant -buildfile ../ant/step1_generate.xml \
    -DDpipeline.properties=pipeline.properties \
    -Dbasedir=.
