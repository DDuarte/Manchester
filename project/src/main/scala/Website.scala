import breeze.stats.distributions.Rand
import org.graphstream.graph._
import org.graphstream.graph.implementations._

import scala.collection.JavaConversions._
import scala.collection.mutable.{HashMap => MHashMap, Set => MSet}

case class Website(pages: Set[Page], homepage: Page, pageTypes: WebsitePageTypes)

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
  protected val purchases = MHashMap[Page /* Product */, Long]()
  protected var uniqueUserCount = 0l
  val users = MHashMap[User, Page]()
  protected var lastUserId = 0

  def visitPage(page: Page) {
    visits += (page -> (visits.getOrElse(page, 0l) + 1l))
  }

  def addToCart(product: Page): Unit = {
    purchases += (product -> (purchases.getOrElse(product, 0l) + 1l))
  }

  def newUser() = {
    uniqueUserCount += 1
  }

  def newUserId = uniqueUserCount + 1

  override def toString: String = {
    val sb = new StringBuilder()

    sb ++= "--- Website statistics ---\n"

    sb ++= "\n- Unique users: " ++= uniqueUserCount.toString ++= "\n"

    sb ++= "\n- Visits:\n"

    visits.toSeq.sortWith(_._2 > _._2).foreach {
      case (page, count) =>
        sb ++= f"${page.id}%15s $count%5d\n"
    }

    sb ++= "\n- Purchases:\n"

    purchases.toSeq.sortWith(_._2 > _._2).foreach {
      case (product, count) =>
        sb ++= f"${product.id}%15s $count%5d\n"
    }

    sb.toString()
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

  override def visitPage(page: Page) = {
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

    super.visitPage(page)
  }
}

case class WebsitePageTypes(
  list: String,
  product: String,
  cart: String,
  generic: String)
