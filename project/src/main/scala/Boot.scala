import MongoHelpers._
import breeze.stats.distributions.{Poisson, Rand}
import org.graphstream.graph._
import org.graphstream.graph.implementations._
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.{BsonArray, BsonString}
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._

import scala.collection.JavaConversions._
import scala.collection.immutable.HashSet
import scala.collection.mutable

abstract class Action
case class BrowseToAction(page: Page) extends Action
case class ExitAction() extends Action
case class AddToCartAction(product: Page /* FIXME */, cartPage: Page) extends Action

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
    if (Rand.randInt(3).draw() == 2 /* 33.3% */ || currentPage.links.isEmpty)
      ExitAction()
    else if (Rand.randInt(101).draw() <= 5 /* 5% */ && currentPage.tags.contains("_product")) {
      val cartPage = currentPage.links.find(l => l.tags.contains("_cart")).get
      AddToCartAction(currentPage, cartPage)
    } else {
      val nextPage = Rand.choose(currentPage.links).draw()
      BrowseToAction(nextPage)
    }
  }
}

case class AffinityUser(id: String, affinities: Map[String, Double] = Map()) extends User {
  override def emitAction(currentPage: Page, website: Website): Action = {
    if (Rand.randInt(3).draw() == 2 /* 33.3% */ || currentPage.links.isEmpty)
      ExitAction()
    else if (Rand.randInt(101).draw() <= 5 /* 5% */ && currentPage.tags.contains("_product")) {
      // assumed that a product page links to a cart page
      val cartPage = currentPage.links.find(l => l.tags.contains("_cart")).get
      AddToCartAction(currentPage, cartPage)
    } else {
      val affSelected = RandHelper.choose(affinities).draw()

      val links = currentPage.links.filter(p => p.tags.contains(affSelected) &&
        (p.tags.contains("_product") || p.tags.contains("_productList")))

      val nextPage = if (links.isEmpty)
        Rand.choose(currentPage.links).draw()
      else
        Rand.choose(links).draw()

      BrowseToAction(nextPage)
    }
  }
}

case class Page(val id: String, val tags: Set[String] = HashSet()) {
  val links: mutable.HashSet[Page] = mutable.HashSet()

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

  var visualizationEnabled = false
  var homepage: Page = null
  protected val visits = mutable.HashMap[Page, Long]()
  protected val purchases = mutable.HashMap[Page /* Product */, Long]()
  protected var uniqueUserCount = 0l

  def addPage(page: Page) {
    pages += page

    if (visualizationEnabled) {
      val node = graph.addNode[Node](page.id)
      node.setAttribute("ui.label", page.id)
      node.setAttribute("ui.size", Double.box(1))
    }
  }

  def addLink(page1: Page, page2: Page): Unit = addLink(page1.id, page2.id)

  def addLink(page1Id: String, page2Id: String) {
    pages.find(p => p.id == page2Id) match {
      case Some(p2) => pages.find(p => p.id == page1Id) match {
        case Some(p1) => p1.links += p2
        case None => println("Page (1) " + page1Id + " not found!")
      }
      case None => println("Page (2) " + page2Id + " not found!")
    }

    if (visualizationEnabled) {
      graph.addEdge(s"$page1Id-$page2Id-${Rand.randInt(1000)}", page1Id, page2Id)
    }
  }

  def visitPage(page: Page) {
    if (visualizationEnabled) {
      val node = graph.getNode[Node](page.id)
      val currSize = node.getAttribute[Double]("ui.size")
      node.setAttribute("ui.size", Double.box(currSize + 0.05))
      normalizeSizes()
    }

    visits += (page -> (visits.getOrElse(page, 0l) + 1l))
  }

  def normalizeSizes(): Unit = {
    if (visualizationEnabled) {
      graph.getNodeIterator[Node].foreach(node => {
        val currSize = node.getAttribute[Double]("ui.size")
        if (currSize > 10)
          node.setAttribute("ui.size", Double.box(currSize / 10.0))
      })
    }
  }

  def addToCart(product: Page): Unit = {
    purchases += (product -> (purchases.getOrElse(product, 0l) + 1l))
  }

  def newUser() = {
    uniqueUserCount += 1
  }

  def getStats: String = {
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

  def displayGraph = if (visualizationEnabled) graph.display()
}

object Main extends App {
  def loadMongoWebsite(): Website = {
    val website = new Website
    website.displayGraph

    val mongoClient = MongoClient("mongodb://localhost")
    val database = mongoClient.getDatabase("kugsha")
    //val collection = database.getCollection("atelierdecamisa-pages")
    val collection = database.getCollection("clickfiel-pages")

    // TODO: fix the double iteration over pages
    collection.find().projection(include("url", "type", "category")).sort(ascending("_id")).results().foreach(doc => {
      val url = doc.get[BsonString]("url").get.getValue

      val pageType = doc.get[BsonString]("type").map(_.asString().getValue) match {
        case Some("list") => "_productList"
        case Some("product") => "_product"
        case Some("cart") => "_cart"
        case Some("generic") => "_generic"
        case _ => " _generic"
      }

      val tags: HashSet[String] = doc.get[BsonArray]("category") match {
        case Some(categories: BsonArray) => HashSet(pageType) ++ categories.getValues.map(_.asString().getValue)
        case None => HashSet[String](pageType)
      }

      val page = Page(url, tags)
      website.addPage(page)

      if (website.homepage == null)
        website.homepage = page
    })

    collection.find().projection(include("url", "outbound")).results().foreach(doc => {
      val url = doc.get[BsonString]("url").get.getValue
      for (out <- doc.get[BsonArray]("outbound").get.getValues) {
        val urlOut = out.asString().getValue
        website.addLink(url, urlOut)
      }
    })

    website
  }

  def loadExampleWebsite(): Website = {
    val website = new Website
    website.displayGraph

    val homepage = Page("homepage", HashSet("_home"))
    val electronics = Page("electronics", HashSet("electro", "_productList"))
    val computers = Page("computers", HashSet("electro", "_product"))
    val lingerie = Page("lingerie", HashSet("cloth", "_product"))
    val tshirts = Page("tshirts", HashSet("cloth", "_product"))
    val football = Page("football", HashSet("sports", "football", "_product"))
    val cloth = Page("cloth", HashSet("cloth", "_productList"))
    val sports = Page("sports", HashSet("sports", "football", "_productList"))
    val cart = Page("cart", HashSet("_cart"))

    website.homepage = homepage

    website.addLink(homepage, electronics)
    website.addLink(homepage, cloth)
    website.addLink(homepage, sports)
    website.addLink(homepage, homepage)

    website.addLink(electronics, computers)
    website.addLink(electronics, homepage)
    website.addLink(electronics, electronics)

    website.addLink(cloth, tshirts)
    website.addLink(cloth, lingerie)
    website.addLink(cloth, homepage)
    website.addLink(cloth, cloth)

    website.addLink(sports, football)
    website.addLink(sports, homepage)
    website.addLink(sports, sports)

    website.addLink(computers, cart)
    website.addLink(computers, homepage)
    website.addLink(computers, electronics)
    website.addLink(computers, computers)

    website.addLink(tshirts, cart)
    website.addLink(tshirts, homepage)
    website.addLink(tshirts, cloth)
    website.addLink(tshirts, lingerie)
    website.addLink(tshirts, tshirts)

    website.addLink(lingerie, cart)
    website.addLink(lingerie, homepage)
    website.addLink(lingerie, cloth)
    website.addLink(lingerie, tshirts)
    website.addLink(lingerie, lingerie)

    website.addLink(football, cart)
    website.addLink(football, homepage)
    website.addLink(football, sports)
    website.addLink(football, football)

    website
  }

  new Simulation {
    // System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer")

    val website = Utilities.time("load website") { loadMongoWebsite() }

    val users = mutable.HashMap[User, Page]()
    var lastUserId = 0

    def newUsers() {

      // val personas = Map(
      //   Map(
      //     "cloth" -> 0.3,
      //     "electro" -> 0.1,
      //     "sports" -> 0.6,
      //     ) -> 1.0,
      //   Map(
      //     "cloth" -> 0.9,
      //     "electro" -> 0.1,
      //     "sports" -> 0.0
      //   ) -> 1.0,
      //   Map(
      //     "cloth" -> 0.2,
      //     "electro" -> 0.6,
      //     "sports" -> 0.2
      //   ) -> 2.0
      // )

      val personas = Map(
        Map(
          "Portáteis" -> 1.0,
          "HP" -> 1.0
        ) -> 1.0
      )

      val distribution = Poisson(25)

      val newUsers = distribution.draw()
      println(s"New users: $newUsers")

      for (i <- 0 until newUsers) {
        // val user = new RandomUser(lastUserId.toString)
        val user = AffinityUser(lastUserId.toString, RandHelper.choose(personas).draw())
        lastUserId += 1
        users.put(user, website.homepage)
        website.visitPage(website.homepage)
        website.newUser()
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
                //println(s"User ${user.id} went from page ${prevPage.id} to ${nextPage.id}")
              case addToCart: AddToCartAction =>
                website.addToCart(addToCart.product)
                website.visitPage(addToCart.cartPage)
                website.visitPage(website.homepage)
                users.update(user, website.homepage)
                //println(s"User ${user.id} added ${addToCart.product.id} to cart, back to homepage")
              case exit: ExitAction =>
                users.remove(user)
                //println(s"User ${user.id} exited")
            }

            sleep(0) // animation purposes
          }

          userInjector()
        }
      }
    }

    schedule(0) {
      userInjector()
    }

    run()

    println(website.getStats)
  }

  def sleep(ms: Int = 50) {
    try { Thread.sleep(ms); } catch { case _: Throwable => }
  }
}
