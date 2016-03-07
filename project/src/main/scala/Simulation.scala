import scala.collection.SortedMap
import scala.collection.immutable.Queue

abstract class Simulation {
  type Action = () => Unit
  type Agenda = SortedMap[Long, Queue[Action]]
  type Trigger = (() => Boolean, () => Unit)

  protected var curTime = 0l
  def currentTime: Long = curTime

  private var agenda: Agenda = SortedMap()
  private var triggers: List[Trigger] = List()

  def schedule(delay: Int = 0)(block: => Unit) {
    val time = currentTime + delay
    agenda += (time -> agenda.getOrElse(time, Queue()).enqueue(() => block))
  }

  def schedule(condition: => Boolean)(action: => Unit) {
    triggers ::= (() => condition, () => action)
  }

  protected def step() {
    if (hasNext) {
      curTime = agenda.head._1

      while (agenda.contains(curTime)) {
        println("*** time = " + currentTime + " ***")
        val actions = agenda(curTime)
        agenda -= curTime
        processActions(actions)
        processTriggers()
      }
    }
  }

  protected def processActions(actions: Seq[Action]) { actions.foreach(_()) }
  protected def processTriggers() { triggers.filter(_._1()).foreach(_._2()) }
  protected def hasNext = agenda.nonEmpty

  def run() {
    schedule(0) {
      println("*** simulation started, time = " + currentTime + " ***")
    }

    while (agenda.nonEmpty) step()
  }
}
