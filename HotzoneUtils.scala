package cse512

object HotzoneUtils {

  def StContains(rectPoints: Array[Double],points: Array[Double]):Boolean = {
    if (points(0)>=rectPoints(0) && points(0)<=rectPoints(2) && points(1)>=rectPoints(1) && points(1)<=rectPoints(3)){
      return true;
    }
    return false;
  }

  // YOU NEED TO CHANGE THIS PART

}
