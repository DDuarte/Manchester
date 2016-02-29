import MongoHelpers._
import breeze.linalg.DenseVector
import breeze.stats.distributions.{Multinomial, Poisson, Rand}
import org.graphstream.graph._
import org.graphstream.graph.implementations._
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.{BsonArray, BsonString}
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._

import scala.collection.JavaConversions._
import scala.collection.mutable

abstract class Action
case class BrowseToAction(page: Page) extends Action
case class ExitAction() extends Action
// case class AddToCartAction() extends Action()

abstract class User() {
  def emitAction(currentPage: Page, website: Website): Action
  val id: String

  def canEqual(other: Any): Boolean = other.isInstanceOf[User]

  override def equals(other: Any): Boolean = other match {
    case that: User =>
      (that canEqual this) &&
        id == that.id
    case _ => false
  }

  override def hashCode(): Int = {
    id.hashCode
  }
}

case class RandomUser(id: String) extends User {
  override def emitAction(currentPage: Page, website: Website): Action = {
    if (Rand.randInt(3).draw() == 2 /* 1/3 */ || currentPage.links.isEmpty)
      ExitAction()
    else {
      val nextPage = Rand.choose(currentPage.links).draw()
      BrowseToAction(nextPage)
    }
  }
}

case class AffinityUser(id: String, affinities: Map[String, Double] = Map()) extends User {
  override def emitAction(currentPage: Page, website: Website): Action = {
    if (Rand.randInt(3).draw() == 2) { // 1/3
      ExitAction()
    } else {
      val affSelected = RandHelper.choose(affinities.keys, affinities.values).draw()

      val nextPage = currentPage.links.find(p => p.id.equalsIgnoreCase(affSelected)).get

      BrowseToAction(nextPage)
    }
  }

  /* val personas = Array(
    Map(
      "lingerie" -> 0.3,
      "electronics" -> 0.7
    ),
    Map(
      "lingerie" -> 0.7,
      "electronics" -> 0.3
    )
  ) */
}

case class Page(val id: String, val tags: List[String] = List()) {
  val links: mutable.MutableList[Page] = mutable.MutableList()

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

class Website {
  protected val pages = mutable.HashSet[Page]()
  protected val graph = {
    val graph = new MultiGraph("Website", false, true)
    // graph.addAttribute("ui.quality")
    // graph.addAttribute("ui.antialias")
    graph.addAttribute("ui.stylesheet", "node {fill-color: red; size-mode: dyn-size;} edge {fill-color:grey;}")
    graph
  }

  def getHomePage = pages.head

  def addPage(page: Page) {
    pages += page
    val node = graph.addNode[Node](page.id)
    node.setAttribute("ui.label", page.id)
    node.setAttribute("ui.size", Int.box(1))
  }

  def addLink(page1: Page, page2: Page) {
    if (!pages.contains(page1))
      addPage(page1)

    if (!pages.contains(page2))
      addPage(page2)

    page1.links += page2
    graph.addEdge(s"${page1.id}-${page2.id}-${Rand.randInt(1000)}", page1.id, page2.id)
  }

  def visitPage(page: Page) {
    val node = graph.getNode[Node](page.id)
    val currSize = node.getAttribute[Int]("ui.size")
    node.setAttribute("ui.size", Int.box(currSize + 1))
  }

  def displayGraph = graph.display()
}

object Main extends App {
  def loadMongoWebsite(): Website = {
    val website = new Website
    website.displayGraph

    val mongoClient = MongoClient("mongodb://sf:sf@ds062898.mongolab.com:62898/kugsha")
    val database = mongoClient.getDatabase("kugsha")
    val collection = database.getCollection("atelierdecamisa-pages")
    collection.find().projection(exclude("content")).sort(ascending("_id")).results().foreach(doc => {
      val url = doc.get[BsonString]("url").get.getValue
      val page = Page(url)
      website.addPage(page)

      for (out <- doc.get[BsonArray]("outbound").get.getValues) {
        val urlOut = out.asString().getValue
        val pageOut = Page(urlOut)
        website.addLink(page, pageOut)
      }
    })

    website
  }

  def loadExampleWebsite(): Website = {
    val website = new Website
    website.displayGraph

    val homePage = Page("homepage", List("_home"))
    val electronics = Page("electronics", List("electro"))
    val computers = Page("computers", List("electro"))
    val lingerie = Page("lingerie", List("cloth"))
    val football = Page("football", List("ball"))
    val cart = Page("cart", List("_cart"))
  
    website.addLink(homePage, electronics)
    website.addLink(homePage, lingerie)
    website.addLink(homePage, homePage)
    website.addLink(electronics, homePage)
    website.addLink(electronics, electronics)
    website.addLink(lingerie, homePage)
    website.addLink(lingerie, lingerie)
    website.addLink(homePage, football)
    website.addLink(football, homePage)
    website.addLink(electronics, cart)
    website.addLink(lingerie, cart)
    website.addLink(football, cart)
    website.addLink(cart, homePage)
    website.addLink(electronics, computers)
    website.addLink(computers, cart)
    website.addLink(computers, homePage)
    // website.addLink(homePage, computers)

    website
  }

  new Simulation {
    System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer")

    val website = loadExampleWebsite()

    val users = mutable.HashMap[User, Page]()
    var lastUserId = 0

    def newUsers() {
      val distribution = Poisson(5)
      // viewHistogram2(distribution)

      val newUsers = distribution.draw()
      println(s"New users: $newUsers")

      for (i <- 0 until newUsers) {
        val user = new RandomUser(lastUserId.toString)
        lastUserId += 1
        users.put(user, website.getHomePage)
      }
    }

    def userInjector() {
      if (currentTime < 10) {
        schedule(1) {
          newUsers()
          users.foreach { case (user: User, page: Page) =>
            val action = user.emitAction(page, website)
            action match {
              case browse: BrowseToAction =>
                val prevPage = page
                val nextPage = browse.page
                users.update(user, nextPage)
                website.visitPage(nextPage)
                sleep() // animation purposes
                println(s"User ${user.id} went from page ${prevPage.id} to ${nextPage.id}")
              case exit: ExitAction =>
                users.remove(user)
                println(s"User ${user.id} exited")
            }
          }

          userInjector()
        }
      }
    }

    schedule(0) {
      userInjector()
    }

    run()
  }

  def sleep(ms: Int = 50) {
    try { Thread.sleep(ms); } catch { case _: Throwable => }
  }
}
