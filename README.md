spark-hbase-client
==================

#### Overview

  Provides a Scala class for handling HBase connections within Spark Apps.
The class is intended for use within Spark executor closures providing proper
HBase parallelism, but also wraps some administrative functions for use by the
driver. Care should be taken to not mix object instances between the driver
and executor.


### Project settings

The project has a GitHub based Maven Repository, which would need an entry 
to either maven settings or the project pom. Currently, GitHub requires 
authentication for its [Packages](https://docs.github.com/en/packages) project.
```
    <repositories>
      <repository>
        <id>spark-hbase-client</id>
        <url>https://maven.pkg.github.com/tcarland/spark-hbase-client</url>
      </repository>
    <repositories>
```

Optionally create a local maven entry from the build of this repo
```
  mvn install:install-file -Dpackaging=jar -DgroupId=com.trace3.hbase \
   -DartifactId=spark-hbase-client -Dversion=1.4.0 \
   -Dfile=target/spark-hbase-client-1.4.0.jar
```

Maven Artifact:
```
<dependency>
  <groupId>com.trace3.hbase</groupId>
  <artifactId>spark-hbase-client</artifactId>
  <version>1.4.0</version>
</dependency>
```
