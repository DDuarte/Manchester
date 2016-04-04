import java.text.DecimalFormat

import breeze.stats.distributions.Rand
import org.graphstream.graph._
import org.graphstream.graph.implementations._

import scala.collection.JavaConversions._
import scala.collection.immutable.ListMap
import scala.collection.mutable.{HashMap => MHashMap, Set => MSet}

case class Website(pages: Set[Page], homepage: Page)

case class Page(id: String, links: MSet[Page], tags: Set[String]) {
  def canEqual(other: Any): Boolean = other.isInstanceOf[Page]

  override def equals(other: Any): Boolean = other match {
    case that: Page =>
      (that canEqual this) &&
        id == that.id
    case _ => false
  }

  override def hashCode(): Int = {
    id.hashCode
  }
}

class WebsiteState(website: Website) {
  protected val visits = MHashMap[Page, Long]()
  protected val visitsPerUser = MHashMap[User, Long]()
  protected val purchases = MHashMap[Page /* Product */ , Long]()
  protected var uniqueUserCount = 0l
  val users = MHashMap[User, Page]()
  protected var lastUserId = 0l

  protected var singleSessionCount = 0l
  protected var sessionCount = 0l
  protected def bounceRate = singleSessionCount.toDouble / sessionCount

  def visitPage(user: User, page: Page) {
    users.put(user, page)
    visits += (page -> (visits.getOrElse(page, 0l) + 1l))
    visitsPerUser += (user -> (visitsPerUser.getOrElse(user, 0l) + 1l))
  }

  def addToCart(product: Page): Unit = {
    purchases += (product -> (purchases.getOrElse(product, 0l) + 1l))
  }

  def exit(user: User): Unit = {
    users.remove(user)

    if (visitsPerUser.getOrElse(user, 0l) == 1) {
      singleSessionCount += 1
    }
  }

  def newUser() = {
    uniqueUserCount += 1
    sessionCount += 1
  }

  def newUserId = uniqueUserCount + 1

  override def toString: String = {
    val sb = new StringBuilder()

    sb ++= "--- Website statistics ---\n"

    sb ++= "\n- Unique users: " ++= uniqueUserCount.toString ++= "\n"

    val decFormat = new DecimalFormat("#.###")
    sb ++= "\n- Bounce rate: " ++= decFormat.format(bounceRate * 100) ++= "%\n"

    sb ++= "\n- Visits:\n"

    visits.toSeq.sortWith(_._2 > _._2).foreach {
      case (page, count) =>
        sb ++= f"${page.id}%15s $count%5d\n"
    }

    /*sb ++= "\n- Visits per user:\n"

    visitsPerUser.toSeq.sortWith(_._2 > _._2).foreach {
      case (user, count) =>
        sb ++= f"${user.id}%15s $count%5d\n"
    }*/

    sb ++= "\n- Purchases:\n"

    purchases.toSeq.sortWith(_._2 > _._2).foreach {
      case (product, count) =>
        sb ++= f"${product.id}%15s $count%5d\n"
    }

    sb.toString()
  }

  def toJson: String = {
    import org.json4s._
    import org.json4s.JsonDSL._
    import org.json4s.native.JsonMethods._

    val json =
      ("uniqueUsers" -> uniqueUserCount) ~
        ("bounceRate" -> bounceRate) ~
        ("visits" ->
          ListMap(visits.toSeq.sortWith(_._2 > _._2).map {
            case (page, count) =>
              page.id -> count
          }: _*)) ~
          ("purchases" ->
            ListMap(purchases.toSeq.sortWith(_._2 > _._2).map {
              case (product, count) =>
                product.id -> count
            }: _*))

    pretty(render(json))
  }
}

class WebsiteStateVisualization(website: Website) extends WebsiteState(website) {
  protected val graph = {
    val graph = new MultiGraph("Website", false, true)
    // graph.addAttribute("ui.quality")
    // graph.addAttribute("ui.antialias")
    graph.addAttribute("ui.stylesheet", "node {fill-color: red; size-mode: dyn-size;} edge {fill-color:grey;}")

    website.pages.foreach(page => {
      val node = graph.addNode[Node](page.id)
      node.setAttribute("ui.label", page.id)
      node.setAttribute("ui.size", Double.box(1))
    })

    website.pages.foreach(page => {
      page.links.foreach(link => {
        graph.addEdge[Edge](s"${page.id}-${link.id}-${Rand.randInt(1000)}", page.id, link.id)
      })
    })

    graph
  }

  def display = graph.display()

  override def visitPage(user: User, page: Page) = {
    super.visitPage(user, page)

    val node = graph.getNode[Node](page.id)
    val currSize = node.getAttribute[Double]("ui.size")
    node.setAttribute("ui.size", Double.box(currSize + 0.05))
    normalizeSizes()

    def normalizeSizes() {
      graph.getNodeIterator[Node].foreach(node => {
        val currSize = node.getAttribute[Double]("ui.size")
        if (currSize > 10)
          node.setAttribute("ui.size", Double.box(currSize / 10.0))
      })
    }
  }
}

object PageTypesTags {
  val list = "_productList"
  val product = "_product"
  val cart = "_cart"
  val generic = "_generic"
}
