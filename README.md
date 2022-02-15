spark-hbase-client
==================

## Overview

The *spark-hbase-client* project provides a Scala lib for handling HBase 
connections within Spark applications. The class is intended for use within 
Spark executor closures providing proper HBase parallelism, but also wraps 
some administrative functions for use by the driver. Care should be taken 
to not mix object instances between the driver and executor.


## Project build

The library is currently intended for *Spark 3.2.x* and *HBase 2.4.x*, which 
supports Scala versions 2.12 or 2.13. Scala-2.11 is no longer supported 
and is dropped from the available profiles, though the project is still 
compatible. By default, the build prefers Scala-2.13 for Spark 3.2.x, but 
Scala-2.12 can be compiled by selecting the profile (and ensuring the 
correct spark version is set).
```
mvn package -Pscala-2.12
```


## Project settings

The project has a GitHub based Maven Repository, which would need an entry 
to either maven settings or the project pom. Currently, GitHub requires 
authentication for its [Packages](https://docs.github.com/en/packages) project.
```xml
  <repositories>
      <repository>
          <id>spark-hbase-client</id>
          <url>https://maven.pkg.github.com/tcarland/spark-hbase-client</url>
      </repository>
  </repositories>
```

Optionally create a local maven entry from the build of this repo
```sh
mvn install:install-file -Dpackaging=jar -DgroupId=com.trace3.hbase \
 -DartifactId=spark-hbase-client -Dversion=1.5.0_2.13 \
 -Dfile=target/spark-hbase-client-1.5.0_2.13.jar
```

Maven Artifact:
```xml
  <properties>
      <scala.binary.version>2.13</scala.binary.version>
      <scala.version>2.13.5</scala.version>
  </properties>

  <dependency>
      <groupId>com.trace3.hbase</groupId>
      <artifactId>spark-hbase-client</artifactId>
      <version>1.5.0_${scala.binary.varsion}</version>
  </dependency>
```
