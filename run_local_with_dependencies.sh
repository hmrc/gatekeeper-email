#!/bin/bash

sm2 --start OBJECT_STORE_STUB EMAIL DATASTREAM DIGITAL_CONTACT_STUB

sbt "run -Drun.mode=Dev -Dhttp.port=9620 $*"
