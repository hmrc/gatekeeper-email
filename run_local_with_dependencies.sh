#!/bin/bash

sm --start OBJECT_STORE_STUB

sbt "run -Drun.mode=Dev -Dhttp.port=9620 $*"
