import breeze.linalg.DenseVector
import breeze.stats.distributions.{Multinomial, Poisson, Rand}
import org.graphstream.graph._
import org.graphstream.graph.implementations._
import org.mongodb.scala.bson.{BsonString, BsonArray}
import org.mongodb.scala.{Document, MongoCollection, MongoDatabase, MongoClient}
import org.mongodb.scala._

import scala.collection.mutable
import MongoHelpers._
import scala.collection.JavaConversions._

abstract class Action
case class BrowseToAction(page: Page) extends Action
case class ExitAction() extends Action
// case class AddToCartAction() extends Action("add_to_cart")

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
      val mult = new Multinomial[DenseVector[Double], Int](DenseVector(affinities.values.toArray))
      val affSelected = affinities.keys.toIndexedSeq(mult.draw())

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

case class Page(id: String, links: mutable.MutableList[Page] = mutable.MutableList(), tags: List[String] = List())

class Website {
  protected val pages: mutable.MutableList[Page] = mutable.MutableList()
  protected val graph: MultiGraph = {
    val graph = new MultiGraph("Website", false, true)
    // graph.addAttribute("ui.quality")
    // graph.addAttribute("ui.antialias")
    graph.addAttribute("ui.stylesheet", "node {fill-color: red; size-mode: dyn-size;} edge {fill-color:grey;}");
    graph
  }

  def getHomePage: Page = pages.head

  def addPage(page: Page): Unit = {
    pages += page
    val node = graph.addNode[Node](page.id)
    node.setAttribute("ui.label", page.id)
    node.setAttribute("ui.size", Int.box(10))
  }

  def addLink(page1: Page, page2: Page): Unit = {
    page1.links += page2
    graph.addEdge(page1.id + "-" + page2.id + "-" + Rand.randInt(1000), page1.id, page2.id)
  }

  def visitPage(page: Page): Unit = {
    val node = graph.getNode[Node](page.id)
    val currSize = node.getAttribute[Int]("ui.size")
    node.setAttribute("ui.size", Int.box(currSize + 1))
  }

  def displayGraph = graph.display()
}

object Main extends App {
  def loadMongoWebsite(): Website = {
    val website = new Website
    // website.displayGraph

    val mongoClient: MongoClient = MongoClient("mongodb://sf:sf@ds062898.mongolab.com:62898/kugsha")
    val database: MongoDatabase = mongoClient.getDatabase("kugsha")
    val collection: MongoCollection[Document] = database.getCollection("atelierdecamisa-pages")
    // val results = collection.find().foreach(doc => {
    collection.find().results().foreach(doc => {
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

    val homePage = Page("homepage")
    val electronics = Page("electronics")
    val computers = Page("computers")
    val lingerie = Page("lingerie")
    val football = Page("football")
    val cart = Page("cart")

    website.addPage(homePage)
    website.addPage(lingerie)
    website.addPage(electronics)
    website.addPage(football)
    website.addPage(cart)
    website.addPage(computers)
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
        //users.put(user, Page("a"))
        users.put(user, website.getHomePage)
      }
    }

    def userInjector() {
      if (currentTime < 100) {
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
                sleep()
                println(s"User ${user.id} went from page ${prevPage.id} to ${nextPage.id}")
              case exit: ExitAction =>
                //users.remove(user)
                //println(s"User ${user.id} exited")
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

  def sleep(): Unit = {
    try { Thread.sleep(50); } catch { case _: Throwable => }
  }

  def sleepLong(): Unit = {
    try { Thread.sleep(5000); } catch { case _: Throwable => }
  }
}
