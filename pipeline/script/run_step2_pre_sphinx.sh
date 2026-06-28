#!/bin/sh

ant -buildfile ../ant/step2_pre_sphinx.xml \
    -DDpipeline.properties=pipeline.properties \
    -Dbasedir=.
