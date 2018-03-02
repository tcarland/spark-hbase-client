spark-hbase-client
==================


#### Overview

  Provides a Scala class for handling HBase connections within Spark Apps.
The class is intended for use within Spark executor closures providing proper 
HBase parallelism, but also wraps some administrative functions for use by the 
driver. Care should be taken to not mix object instances between the driver 
and executor.


#### Install

  This project currently lacks a public maven artifact, but can be 
installed locally via the following command: 

```
mvn install:install-file -Dpackaging=jar -DgroupId=com.trace3.hbase -DartifactId=spark-hbase-client -Dversion=1.1.0 -Dfile=spark-hbase-client-1.0.2.jar

# or

mvn install:install-file -Dfile=<path-to-file>
```


Maven Artifact:

```
<dependency>
  <groupId>com.trace3.hbase</groupId>
  <artifactId>spark-hbase-client</artifactId>
  <version>1.1.0</version>
</dependency>
```
