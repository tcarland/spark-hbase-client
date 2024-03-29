#!/bin/bash
#
export HADOOP_CLASSPATH="$HBASE_HOME/conf:$HBASE_HOME/lib/*"

TARGET_JAR="target/spark-hbase-client-test-1.5.3.jar"
TARGET_CLASS="HBaseTest"

spark-submit --master yarn \
 --class $TARGET_CLASS \
 $TARGET_JAR $@
