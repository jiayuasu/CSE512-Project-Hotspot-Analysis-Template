package cse512

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.functions._

object HotcellAnalysis {
  Logger.getLogger("org.spark_project").setLevel(Level.WARN)
  Logger.getLogger("org.apache").setLevel(Level.WARN)
  Logger.getLogger("akka").setLevel(Level.WARN)
  Logger.getLogger("com").setLevel(Level.WARN)

  def runHotcellAnalysis(spark: SparkSession, pointPath: String): DataFrame =
  {

    // Load the original data from a data source
    var pickupInfo = spark.read.format("com.databricks.spark.csv").option("delimiter",";").option("header","false").load(pointPath);
    pickupInfo.createOrReplaceTempView("nyctaxitrips")
    //pickupInfo.show()

    // Assign cell coordinates based on pickup points
    spark.udf.register("CalculateX",(pickupPoint: String)=>((
      HotcellUtils.CalculateCoordinate(pickupPoint, 0)
      )))
    spark.udf.register("CalculateY",(pickupPoint: String)=>((
      HotcellUtils.CalculateCoordinate(pickupPoint, 1)
      )))
    spark.udf.register("CalculateZ",(pickupTime: String)=>((
      HotcellUtils.CalculateCoordinate(pickupTime, 2)
      )))
    pickupInfo = spark.sql("select CalculateX(nyctaxitrips._c5),CalculateY(nyctaxitrips._c5), CalculateZ(nyctaxitrips._c1) from nyctaxitrips")
    var newCoordinateName = Seq("x", "y", "z")
    pickupInfo = pickupInfo.toDF(newCoordinateName:_*)
    //pickupInfo.show()

    // Define the min and max of x, y, z
    val minX = -74.50/HotcellUtils.coordinateStep
    val maxX = -73.70/HotcellUtils.coordinateStep
    val minY = 40.50/HotcellUtils.coordinateStep
    val maxY = 40.90/HotcellUtils.coordinateStep
    val minZ = 1
    val maxZ = 31
    val numCells = (maxX - minX + 1)*(maxY - minY + 1)*(maxZ - minZ + 1)


    // YOU NEED TO CHANGE THIS PART
    pickupInfo.createOrReplaceTempView("nyctaxitrips")

    pickupInfo = spark.sql("select * from nyctaxitrips")

    pickupInfo = pickupInfo.select(concat(pickupInfo.col("x"), lit(","), pickupInfo.col("y"),lit(","), pickupInfo.col("z")).alias("cellVal"))
    pickupInfo = pickupInfo.groupBy("cellVal").agg(count("cellVal").alias("hotness"))
    pickupInfo.createOrReplaceTempView("nyctaxitrips")

    val meanValue =  pickupInfo.agg(sum("hotness")).first.getLong(0).*(1.0)./(numCells)
    val standardDev = scala.math.sqrt(pickupInfo.withColumn("hotness2", pow("hotness", 2)).agg(sum("hotness2")).first.getDouble(0)./(numCells).-(meanValue.*(meanValue)))
    val pickupInfoMap = pickupInfo.collect().map(row => (row.getString(0),row.getLong(1))).toMap

    //val  keys  =  pickupInfoMap.keys
    //val values  = pickupInfoMap.values
    //print("keys"+ keys + "values"  + values);

    spark.udf.register("gScore",(cellVal: String)=> HotcellUtils.gScoreCalculate(cellVal, numCells.toInt, pickupInfoMap, minX.toInt, maxX.toInt, minY.toInt, maxY.toInt, minZ.toInt,   maxZ.toInt, meanValue, standardDev ))

    pickupInfo = spark.sql("select nyctaxitrips.cellVal,gScore(nyctaxitrips.cellVal) as gScore from nyctaxitrips")
    pickupInfo = spark.sql("select nyctaxitrips.cellVal,gScore(nyctaxitrips.cellVal) as gScore from nyctaxitrips")
    //pickupInfo = spark.sql("select gScore(nyctaxitrips.cellVal) as gScore from nyctaxitrips")
    pickupInfo = pickupInfo.sort(desc("gScore"))
    pickupInfo = pickupInfo.selectExpr("split(cellVal, ',')[0] as x","split(cellVal, ',')[1] as y","split(cellVal, ',')[2] as z")

    print("end gscore")
    return pickupInfo
  }
}