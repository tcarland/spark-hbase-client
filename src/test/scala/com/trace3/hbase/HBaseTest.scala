package com.trace3.hbase

import org.apache.spark.sql.SparkSession

import org.apache.hadoop.hbase.HConstants
import org.apache.hadoop.hbase.mapreduce.{TableInputFormat, TableOutputFormat}


object HBaseTest {

  val usage : String = """
    | ==> Usage: HBaseTest <zookeepers> <cmd> <table_name>
    | ==>   where cmd =  list|create|delete
    """.stripMargin

  
  def main ( args: Array[String] ) {
    if (args.length < 1) {
      System.err.println(usage)
      System.exit(1)
    }

    val zks : Array[String] = args(0).split(",")
    val zk = zks(0).split(":")
    
    if ( zk.length < 2 ) {
      System.err.println("  ==> Error in Zookeeper definition. Should be 'zkHost1:zkPort,zkHost2:zkPort'")
      System.err.println(usage)
      System.exit(1)
    }

    val spark = SparkSession.builder.appName("HBaseClientTest").getOrCreate()
    val sc    = spark.sparkContext
    val hbc   = new HBaseClient(zk(0), zk(1))

    // List and exit
    if ( args(1).equals("list") ) {
      val ary = hbc.listTables
      println(" ====> List Tables:")
      ary.foreach(println)
      System.exit(0)
    }

    if (args.length < 2) {
      System.err.println(usage)
      System.exit(1)
    }
    
    val tbl = args(2)
   
    if ( hbc.tableExists(tbl) )
      println("  ==> Table '" + tbl + "' Exists")
    else
      println("  ==> Table '" + tbl + "' not found")

    if ( args(1).toLowerCase().equals("create") ) 
    {
      if ( args.length < 3 ) {
        System.err.println(usage)
        System.exit(1)
      }

      if ( hbc.createTable(tbl, args(2)) )
        println("  ==> Table created")
    } 
    else if ( args(0).toLowerCase().equals("delete") ) 
    {
      hbc.deleteTable(tbl)
      if ( hbc.tableExists(tbl) )
        println("  ==> ERROR..delete no workie?")
    } else {
      // Other options for configuring scan behavior are available. More information available at
      // http://hbase.apache.org/apidocs/org/apache/hadoop/hbase/mapreduce/TableInputFormat.html
      val conf = hbc.getConfiguration

      val scannerTimeout = conf.getLong(HConstants.HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD, -1)
      println("Current (local) lease period: " + scannerTimeout + "ms")

      conf.set(TableInputFormat.INPUT_TABLE, tbl)

      val hbaseRDD = sc.newAPIHadoopRDD(conf, classOf[TableInputFormat],
        classOf[org.apache.hadoop.hbase.io.ImmutableBytesWritable],
        classOf[org.apache.hadoop.hbase.client.Result])

      hbaseRDD.take(15).foreach(println)
    }

    spark.stop()
  }
}
