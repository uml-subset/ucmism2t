#!/bin/sh

ant -buildfile ../ant/step3_sphinx.xml \
    -Dproperties=pipeline.properties \
    -Dbasedir=.
