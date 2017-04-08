/**   HBaseClient.scala
 *
 *    @author Timothy C. Arland <tarland@trace3.com>
 *    @created July 4, 2016
**/
package com.trace3.hbase

import org.apache.spark.rdd.RDD

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Job

import org.apache.hadoop.hbase.{TableName, HConstants, KeyValue}
import org.apache.hadoop.hbase.{HBaseConfiguration, HColumnDescriptor, HTableDescriptor}
import org.apache.hadoop.hbase.client.{HBaseAdmin, Table, ConnectionFactory, Connection}
import org.apache.hadoop.hbase.client.{Put, Result, Get, Delete, Scan, ResultScanner}
import org.apache.hadoop.hbase.mapreduce.{TableInputFormat, TableOutputFormat, HFileOutputFormat2}
import org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.io.compress.Compression._
import org.apache.hadoop.hbase.regionserver.BloomType
import org.apache.hadoop.hbase.util.Bytes


/**  HBaseClient wraps use of the org.apache.hadoop.hbase API. 
  *  This should not be considered thread-safe and care must 
  *  be used regarding context ie. driver vs worker. Many of 
  *  these methods can also all throw java.io.IOException.
  *
  *  TODO:  
  *  Clean up and provide better exception/error handling
  *  Take an arbitrary list (String*) of zookeepers
  *  Provide better connection status and zookeeper fallover
  *         
 **/
class HBaseClient ( zkHost: String, zkPort: String ) extends Serializable {

  var conf: Configuration = HBaseConfiguration.create
  var blockSize: Int      = 64 * 1024
  var compress: Boolean   = false
  var compressType        = Algorithm.SNAPPY
  var bloomFilter         = BloomType.ROW
  val version             = "1.0.2"

  init()
 
  val conn: Connection   = ConnectionFactory.createConnection(conf)

  
  private def init() : Unit = {
    this.conf.set("hbase.zookeeper.quorum", zkHost)
    this.conf.set("hbase.zookeeper.property.clientPort", zkPort)
  }

  
  def getConfiguration : Configuration = this.conf
  def getConnection    : Connection    = this.conn
  def close() : Unit                   = this.conn.close()


  def setRPCTimeout ( timeOut : Long )   : Unit = this.setRPCTimeout(timeOut.toString)
  def setRPCTimeout ( timeOut : String ) : Unit = this.conf.set("hbase.rpc.timeout", timeOut)
  def getRPCTimeout : String                    = this.conf.get("hbase.rpc.timeout")


  def setScannerTimeout ( timeOut : Long ) : Unit = 
    this.conf.setLong(HConstants.HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD, timeOut)
  def setScannerTimeout ( timeOut : String ) : Unit =
    this.setScannerTimeout(timeOut.toLong)
  def getScannerTimeout : Long =
    this.conf.getLong(HConstants.HBASE_CLIENT_SCANNER_TIMEOUT_PERIOD, -1)


  /*  BloomType.NONE, BloomType.ROW, or BloomType.ROWCOL */
  def setBloomFilterType ( filter: BloomType ) : Unit = this.bloomFilter = filter
  def getBloomFilterType : BloomType                  = this.bloomFilter


  /*  Set compression type (eg. SNAPPY) */
  def setCompressionType ( algo: Algorithm ) : Unit = this.compressType = algo
  def getCompressionType : Algorithm                = this.compressType
  def enableCompression  ( c: Boolean ) : Unit      = this.compress = c
  def compressionEnabled : Boolean                  = this.compress


  def setInputTable ( tableName : String ) : Unit =
    this.conf.set(TableInputFormat.INPUT_TABLE, tableName)


  /** Returns an Array of table names as strings */
  def listTables : Array[String] = {
    val admin = this.conn.getAdmin
    val ary   = admin.listTableNames.map(t => t.toString)
    admin.close()
    ary
  }


  /** Returns an Array of org.apache.hadoop.hbase.HTableDescriptors */
  def getTables : Array[HTableDescriptor] = {
    val admin   = this.conn.getAdmin
    val tables  = admin.listTables()
    admin.close()
    tables
  }


  /** Returns the HBase Table handle for a table */
  def getTable ( tableName: String ) : Table = 
    this.conn.getTable(TableName.valueOf(tableName))


  /**  Create a new table
    *
    * @param tableName   Name of the new table
    * @param colFamily   Name of the column family
    * @param regionKeys  A list of keys to split by
    * @return            A Boolean indicating success of the operation
    */
  def createTable ( tableName: String, 
                    colFamily: String, 
                    regionKeys: Seq[String] = Seq.empty ) : Boolean =
  {
    val admin = this.conn.getAdmin

    if ( admin.isTableAvailable(TableName.valueOf(tableName)) )
      return false

    var tDesc = new HTableDescriptor(TableName.valueOf(Bytes.toBytes(tableName)))
    val cDesc = new HColumnDescriptor(Bytes.toBytes(colFamily))

    if ( this.compress )
      cDesc.setCompressionType(this.compressType)

    cDesc.setBlocksize(this.blockSize)
    cDesc.setBloomFilterType(this.bloomFilter)
    tDesc.addFamily(cDesc)

    if ( regionKeys.isEmpty )
      admin.createTable(tDesc)
    else {
      val regionKeyBytes = regionKeys.map(Bytes.toBytes).toArray
      admin.createTable(tDesc, regionKeyBytes)
    }

    admin.close()
    true
  }


  /** Modifies an existing table/column family updating table options */
  def modifyTable ( tableName: String, colFamily: String ) : Unit = {
    val admin = this.conn.getAdmin
    val table = TableName.valueOf(tableName)
    var tDesc = admin.getTableDescriptor(table)
    val cDesc = tDesc.getFamily(Bytes.toBytes(colFamily))

    if ( this.compress )
      cDesc.setCompressionType(this.compressType)

    cDesc.setBlocksize(this.blockSize)
    cDesc.setBloomFilterType(this.bloomFilter)

    tDesc.addFamily(cDesc)

    if ( admin.isTableAvailable(table) )
      admin.disableTable(table)

    admin.modifyTable(table, tDesc)
    admin.enableTable(table)
    admin.close()
  }


  /** Add a new ColumnFamily to the given HBase Table.
   *  Note the underlying modifyTable is asynchronous. 
   *
   *  TODO: This method should properly handle the Future<> 
   *  return object and offer switchable behavior to either 
   *  block until complete, or return immediately/asynchronous.
   *
   *  @param tableName  name of the table
   *  @param colFamily  name of the column family to add
  **/
  def addFamily ( tableName: String, colFamily: String ) : Unit = {
    val admin = this.conn.getAdmin
    val table = TableName.valueOf(tableName)

    var tDesc = admin.getTableDescriptor(table)
    val cDesc = new HColumnDescriptor(Bytes.toBytes(colFamily))

    if ( this.compress )
      cDesc.setCompressionType(this.compressType)

    cDesc.setBlocksize(this.blockSize)
    cDesc.setBloomFilterType(this.bloomFilter)

    tDesc.addFamily(cDesc)

    if ( admin.isTableAvailable(table) )
      admin.disableTable(table)

    admin.modifyTable(table, tDesc)
    admin.enableTable(table)
    admin.close()
  }


  /** Determines if a given table is available */
  def exists      ( tableName: String ) : Boolean = this.tableExists(tableName)
  def tableExists ( tableName: String ) : Boolean = {
    val admin = this.conn.getAdmin
    val res   = admin.isTableAvailable(TableName.valueOf(tableName))
    admin.close()
    res
  }


  /** Deletes an existing table from HBase */
  def dropTable   ( tableName: String ) : Unit = this.deleteTable(tableName)
  def deleteTable ( tableName: String ) : Unit = {
    val admin = this.conn.getAdmin
    val table = TableName.valueOf(tableName)
    admin.disableTable(table)
    admin.deleteTable(table)
    admin.close()
  }


  /**  Saves a given RDD of HBase Puts to the provided table */
  def saveDataset ( tableName: String, 
                    dataSet: RDD[(ImmutableBytesWritable, Put)] ) : Unit = 
  {
    this.conf.set("hbase.mapred.outputtable", tableName)
    this.conf.set("mapreduce.job.outputformat.class", 
                  "org.apache.hadoop.hbase.mapreduce.TableOutputFormat")
    this.conf.set("mapreduce.job.output.key.class", 
                  "org.apache.hadoop.hbase.io.ImmutableBytesWritable")
   
    dataSet.saveAsNewAPIHadoopDataset(this.conf)
  }


  /** Computes the region splits based on given number of regions
    * for the provided RDD of (sorted!) string keys
    */
  def computeRegionSplits ( dataSet: RDD[String], numRegions: Int ) : Seq[String] = 
    dataSet.mapPartitions(_.take(1)).collect.toList.tail


  /** Note that the HBase Bulk Loader requires the keys to be sorted, and
    * the tmpPath should not already exist.
    */
  def saveBulkDataset ( tmpPath: String, tableName: String, 
                        dataSet: RDD[(ImmutableBytesWritable, KeyValue)] ) : Unit =
  {
    this.conf.set(TableOutputFormat.OUTPUT_TABLE, tableName)

    val job = Job.getInstance(this.conf)
    job.setMapOutputKeyClass(classOf[ImmutableBytesWritable])
    job.setMapOutputValueClass(classOf[KeyValue])

    HFileOutputFormat2.configureIncrementalLoad(job, this.getTable(tableName),
      this.conn.getRegionLocator(TableName.valueOf(tableName)))

    dataSet.saveAsNewAPIHadoopFile(tmpPath, classOf[ImmutableBytesWritable], classOf[KeyValue],
      classOf[HFileOutputFormat2], job.getConfiguration)
  }


  /**  Completes the bulk loading by processing the HFiles created 
    *  by saveBulkDataset().
    * 
    *  @param tmpPath  is the path where the bulk HFiles were written
    *  @param tableName is the name of the table to load the HFiles 
   **/
  def doBulkLoad ( tmpPath: String, tableName: String ) : Unit = {
    val admin  = this.conn.getAdmin
    val loader = new LoadIncrementalHFiles(this.conf)

    try {
      loader.doBulkLoad(new Path(tmpPath), admin, this.getTable(tableName),
        this.conn.getRegionLocator(TableName.valueOf(tableName)))
    } finally {
      admin.close()
    }
  }

} // class HBaseClient

