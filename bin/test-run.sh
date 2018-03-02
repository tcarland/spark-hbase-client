#!/bin/bash
#

export HADOOP_CLASSPATH="$HBASE_HOME/conf:$HBASE_HOME/lib/*"


TARGET_JAR="target/spark-hbase-client-test-1.1.0.jar"
TARGET_CLASS="com.trace3.hbase.HBaseTest"


spark-submit --master yarn \
--num-executors 4 \
--class $TARGET_CLASS \
$TARGET_JAR $@
