#!/usr/bin/env bash
sbt clean compile coverage test IntegrationTest/test coverageReport
