package cse512

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import scala.collection.mutable.ListBuffer

object HotcellUtils {
  val coordinateStep = 0.01

  def CalculateCoordinate(inputString: String, coordinateOffset: Int): Int =
  {
    // Configuration variable:
    // Coordinate step is the size of each cell on x and y
    var result = 0
    coordinateOffset match
    {
      case 0 => result = Math.floor((inputString.split(",")(0).replace("(","").toDouble/coordinateStep)).toInt
      case 1 => result = Math.floor(inputString.split(",")(1).replace(")","").toDouble/coordinateStep).toInt
      // We only consider the data from 2009 to 2012 inclusively, 4 years in total. Week 0 Day 0 is 2009-01-01
      case 2 => {
        val timestamp = HotcellUtils.timestampParser(inputString)
        result = HotcellUtils.dayOfMonth(timestamp) // Assume every month has 31 days
      }
    }
    return result
  }

  def timestampParser (timestampString: String): Timestamp =
  {
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    val parsedDate = dateFormat.parse(timestampString)
    val timeStamp = new Timestamp(parsedDate.getTime)
    return timeStamp
  }

  def dayOfYear (timestamp: Timestamp): Int =
  {
    val calendar = Calendar.getInstance
    calendar.setTimeInMillis(timestamp.getTime)
    return calendar.get(Calendar.DAY_OF_YEAR)
  }

  def dayOfMonth (timestamp: Timestamp): Int =
  {
    val calendar = Calendar.getInstance
    calendar.setTimeInMillis(timestamp.getTime)
    return calendar.get(Calendar.DAY_OF_MONTH)
  }

  // YOU NEED TO CHANGE THIS PART
  def gScoreCalculate(cellStr: String, cellCount: Int, mapHotCell: Map[String, Long], minXVal: Int, maxXVal: Int , minYVal: Int, maxYVal: Int, minZVal: Int, maxZVal: Int, meanVal : Double, standardDeviationVal : Double): Double =
  {

    var cellNeighbors = new ListBuffer[Long]()

    val cellX :: cellY :: cellZ :: _ = cellStr.split(",").toList
    for(timeStep <- cellZ.toInt.-(1) to cellZ.toInt.+(1)) {

      for(latitudeVal <- cellX.toInt.-(1) to cellX.toInt.+(1)) {

        for(longitudeVal <- cellY.toInt.-(1) to cellY.toInt.+(1)) {

          if(latitudeVal >= minXVal && latitudeVal <= maxXVal && longitudeVal >= minYVal && longitudeVal <= maxYVal && timeStep >= minZVal && timeStep <= maxZVal) {

            if (mapHotCell.contains(latitudeVal.toString +','+ longitudeVal.toString +','+ timeStep.toString))

              cellNeighbors += mapHotCell(latitudeVal.toString +','+ longitudeVal.toString +','+ timeStep.toString)

            else

              cellNeighbors += 0
          }
        }
      }
    }
    val gScore = cellNeighbors.sum.-(meanVal.*(cellNeighbors.size))./(standardDeviationVal.*(scala.math.sqrt((cellNeighbors.size).*(cellCount).-((cellNeighbors.size).*(cellNeighbors.size))./(cellCount.-(1)))))
    return gScore
  }
}