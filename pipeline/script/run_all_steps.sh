#!/bin/sh

ant -buildfile ../ant/all_steps.xml \
    -DDpipeline.properties=pipeline.properties \
    -Dbasedir=.
