import scala.collection.SortedMap
import scala.collection.immutable.Queue
import scala.util.Random

case class Persona(affinities: Map[String, Double] = Map())
case class User(id: String, p: Persona) {
  def init(): Unit = {

  }
}

case class Page(id: String, links: List[Page] = List(), events: Map[Event, Double] = Map())

class Event(val id: Int, f: () => List[(Int, Event)]) {

}

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
    while (agenda.nonEmpty) step()
  }
}

trait SimulationStatistics extends Simulation {
  private var numActions: Int = 0

  override def processActions(actions: Seq[Action]) {
    numActions += actions.size
    super.processActions(actions)
  }

  def actionCount = numActions
}

object Main extends App {
  new Simulation {
    val electronics = Page("electronics")
    val lingerie = Page("lingerie")
    val homePage = Page("homepage", List(electronics, lingerie))

    def userInjector() {
      if (currentTime < 100) {
        schedule(1) {
          println("New users: " + Random.nextInt(5))
          userInjector()
        }
      }
    }

    schedule(0) { userInjector() }

    run()
  }
}
