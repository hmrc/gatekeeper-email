#!/bin/bash

sm2 --start EMAIL DATASTREAM DIGITAL_CONTACT_STUB

sbt "run -Drun.mode=Dev -Dhttp.port=9620 $*"
