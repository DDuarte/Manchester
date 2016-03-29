object Utilities {
  def time[R](str: String = "")(block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block
    val t1 = System.currentTimeMillis()
    println("Elapsed time (" + str + "): " + (t1 - t0) + " ms")
    result
  }
}
