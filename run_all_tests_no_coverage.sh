#!/usr/bin/env bash
sbt clean compile test it:test
python dependencyReport.py gatekeeper-email
