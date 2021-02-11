#!/usr/bin/env bash
set -e

mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:tree -f ../../pom.xml -DoutputFile="docs/dependency-graph-full/dependency-graph-full.txt"