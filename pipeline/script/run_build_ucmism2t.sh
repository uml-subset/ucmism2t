#!/bin/sh

ant -buildfile ../ant/build_ucmism2t.xml \
    -Dpipeline.properties=pipeline.properties \
    -Dplatform=linux \
    -Dbasedir=.
