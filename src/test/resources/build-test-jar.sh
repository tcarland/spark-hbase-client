#!/bin/bash
#  Build a test jar
#  Run 'mvn package test' first
#

TARGET_JAR="target/spark-hbase-client-test-1.1.7.jar"

( jar -cvf $TARGET_JAR -C target/classes . -C target/test-classes . )
