import breeze.linalg.DenseVector
import breeze.stats.distributions.{Multinomial, Rand}

object RandHelper {
  def choose[T](c: Iterable[T], weights: Iterable[Double]) : Rand[T] = new Rand[T] {
    def draw() = {
      require(c.nonEmpty && weights.nonEmpty, "collections cannot be empty")

      val indexed = c.toIndexedSeq
      val weightsNArray = normalize(weights).toArray

      require(indexed.size == weightsNArray.length, "collections need to have the same size")

      val mult = new Multinomial[DenseVector[Double], Int](DenseVector(weightsNArray))
      indexed(mult.draw())
    }

    private def normalize(weights: Iterable[Double]): Iterable[Double] = {
      val sum = weights.sum
      weights.map(w => w / sum)
    }
  }
}
