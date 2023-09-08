#!/bin/bash

sm2 --start OBJECT_STORE_STUB EMAIL DATASTREAM

sbt "run -Drun.mode=Dev -Dhttp.port=9620 $*"
