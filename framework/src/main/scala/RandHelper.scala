import breeze.linalg.DenseVector
import breeze.stats.distributions.{Multinomial, Rand}

object RandHelper {
  def choose[T](weightsMap: Map[T, Double]): Rand[T] = new Rand[T] {
    def draw() = {
      require(weightsMap.nonEmpty, "map cannot be empty")

      val indexed = weightsMap.keys.toIndexedSeq
      val weightsNArray = normalize(weightsMap.values).toArray

      val mult = new Multinomial[DenseVector[Double], Int](DenseVector(weightsNArray))
      indexed(mult.draw())
    }

    private def normalize(weights: Iterable[Double]): Iterable[Double] = {
      val sum = weights.sum
      weights.map(w => w / sum)
    }
  }
}
