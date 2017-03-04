#!/bin/bash
#


export HADOOP_CLASSPATH="$HBASE_HOME/conf:$HBASE_HOME/lib/*"

spark-submit --master yarn \
--num-executors 4 \
--class com.trace3.bdi.HBaseTest \
target/hbase-client-test-1.0.jar $@



