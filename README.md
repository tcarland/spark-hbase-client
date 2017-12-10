spark-hbase-client
==================


#### Overview

  Provides a Scala class for handling HBase connections and various administrative 
actions. The class is intended for use within Spark executor closures for proper 
HBase parallelism, but also wraps some administrative functions for use by the 
driver, but care should be taken to not mix object instances between the two, 
driver and executor.

#### Install

  This can be installed to a local maven repository by running the following maven command.

```
mvn install:install-file -Dpackaging=jar -DgroupId=com.trace3.hbase -DartifactId=spark-hbase-client -Dversion=1.0.2 -Dfile=spark-hbase-client-1.0.2.jar

or 

mvn install:install-file -Dfile=<path-to-file>
```


Maven Artifact:
 
```
<dependency>
  <groupId>com.trace3.hbase</groupId>
  <artifactId>spark-hbase-client</artifactId>
  <version>1.0.2</version>
</dependency>
```


