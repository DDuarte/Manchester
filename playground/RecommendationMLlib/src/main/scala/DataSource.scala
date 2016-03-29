import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

case class DataSourceParams(appId: Int)

class DataSource(val dsp: DataSourceParams) {
  def readTraining(sc: SparkContext): TrainingData = {

    val data: RDD[String] = sc.textFile("data/mllib/als/test.data")
    val ratings = data.map(_.split(',') match { case Array(user, item, event) => {
      val ratingValue: Double = event match {
        case "view" => 1.0
        case "pageView" => 1.0
        case "productView" => 1.5
        case "add" => 1.0
        case "productClickPaid" => 3.0
        case "buy" => 3.0 // map buy event to rating value of 4
        case _ => 0.0/* throw new Exception(s"Unexpected event $event is read.") */
      }
      Rating(user, item, ratingValue)
    }})

    new TrainingData(ratings)
  }
}

case class Rating(
                   user: String,
                   item: String,
                   rating: Double
                 )

class TrainingData(
                    val ratings: RDD[Rating]
                  ) extends Serializable {
  override def toString = {
    s"ratings: [${ratings.count()}] (${ratings.take(2).toList}...)"
  }
}
