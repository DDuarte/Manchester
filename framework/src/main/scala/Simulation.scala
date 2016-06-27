import scala.collection.SortedMap
import scala.collection.immutable.Queue

abstract class Simulation {
  type Action = () => Unit

  protected var curTime = 0l
  def currentTime: Long = curTime

  private var agenda: SortedMap[Long, Queue[Action]] = SortedMap()

  def schedule(delay: Int = 0)(block: => Unit) {
    val time = currentTime + delay
    agenda += (time -> agenda.getOrElse(time, Queue()).enqueue(() => block))
  }

  protected def step() {
    if (hasNext) {
      curTime = agenda.head._1

      while (agenda.contains(curTime)) {
        //println("*** time = " + currentTime + " ***")
        val actions = agenda(curTime)
        agenda -= curTime
        processActions(actions)
      }
    }
  }

  protected def processActions(actions: Seq[Action]) { actions.foreach(_ ()) }
  protected def hasNext = agenda.nonEmpty

  def run() {
    schedule(0) {
      println("*** simulation started, time = " + currentTime + " ***")
    }

    while (agenda.nonEmpty) step()
  }
}
