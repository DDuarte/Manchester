import org.apache.spark.{SparkConf, SparkContext}

case class Query(
    user: String,
    num: Int
)
    extends Serializable

case class PredictedResult(
    itemScores: Array[ItemScore]
)
    extends Serializable

case class ItemScore(
    item: String,
    score: Double
)
    extends Serializable

object RecommendationExample {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setAppName("CollaborativeFilteringExample")
      .setMaster("local[1]")
      .set("spark.ui.enabled", "false")

    val sc = new SparkContext(conf)

    val params = ALSAlgorithmParams(10, 10, 0.01, None)
    val als = new ALSAlgorithm(params)
    val model =
      als.train(sc, new DataSource(DataSourceParams(1)).readTraining(sc))
    val result =
      als.predict(model, Query("77870f08f9c441f4b99204caed4789c9", 10))
    println(result.itemScores.mkString("\n"))
  }
}
