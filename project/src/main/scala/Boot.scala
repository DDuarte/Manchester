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
import scala.collection.mutable.{Set => MSet, HashMap => MHashMap}
import com.typesafe.config.ConfigFactory

abstract class Action
case class BrowseToAction(page: Page) extends Action
case class ExitAction() extends Action
case class AddToCartAction(product: Page /* FIXME */, cartPage: Page) extends Action

abstract class User(val id: String) {
  def emitAction(currentPage: Page, website: Website): Action

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

case class RandomUser(userId: String) extends User(userId) {
  override def emitAction(currentPage: Page, website: Website): Action = {
    if (Rand.randInt(3).draw() == 2 /* 33.3% */ || currentPage.links.isEmpty)
      ExitAction()
    else if (Rand.randInt(101).draw() <= 5 /* 5% */ && currentPage.tags.contains(website.pageTypes.product)) {
      val cartPage = currentPage.links.find(l => l.tags.contains(website.pageTypes.cart)).get
      AddToCartAction(currentPage, cartPage)
    } else {
      val nextPage = Rand.choose(currentPage.links).draw()
      BrowseToAction(nextPage)
    }
  }
}

case class AffinityUser(userId: String, affinities: Map[String, Double] = Map()) extends User(userId) {
  override def emitAction(currentPage: Page, website: Website): Action = {
    if (Rand.randInt(3).draw() == 2 /* 33.3% */ || currentPage.links.isEmpty)
      ExitAction()
    else if (Rand.randInt(101).draw() <= 5 /* 5% */ && currentPage.tags.contains(website.pageTypes.product)) {
      // assumed that a product page links to a cart page
      val cartPage = currentPage.links.find(l => l.tags.contains(website.pageTypes.cart)).get
      AddToCartAction(currentPage, cartPage)
    } else {
      val affSelected = RandHelper.choose(affinities).draw()

      val links = currentPage.links.filter(p => p.tags.contains(affSelected) &&
        (p.tags.contains(website.pageTypes.product) || p.tags.contains(website.pageTypes.list)))

      val nextPage = if (links.isEmpty)
        Rand.choose(currentPage.links).draw()
      else
        Rand.choose(links).draw()

      BrowseToAction(nextPage)
    }
  }
}

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

case class Website(pages: Set[Page], homepage: Page, pageTypes: WebsitePageTypes)

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

object Main extends App {
  val config = ConfigFactory.load()

  val pageTypes = WebsitePageTypes(
    config.getString("types.list"),
    config.getString("types.product"),
    config.getString("types.cart"),
    config.getString("types.generic")
  )

  def loadMongoWebsite(): Website = {
    val mongoClient = MongoClient(config.getString("mongodb.url"))
    val database = mongoClient.getDatabase(config.getString("mongodb.db"))
    val collection = database.getCollection(config.getString("mongodb.collection"))

    var homepageId: String = null

    val pages: Map[String, (Page, Set[String])] = Map(collection
      .find()
      .projection(include("url", "type", "category", "outbound"))
      .sort(ascending("_id"))
      .results()
      .map { doc => {
        val url = doc.get[BsonString]("url").get.getValue

        if (homepageId == null) homepageId = url

        val pageType = doc.get[BsonString]("type").map(_.asString().getValue) match {
          case Some("list") => pageTypes.list
          case Some("product") => pageTypes.product
          case Some("cart") => pageTypes.cart
          case Some("generic") => pageTypes.generic
          case Some(t) =>
            Console.err.println(s"Page $url has unknown type $t")
            pageTypes.generic
          case None =>
            Console.err.println(s"Page $url has no type")
            pageTypes.generic
        }

        val tags: HashSet[String] = doc.get[BsonArray]("category") match {
          case Some(categories: BsonArray) => HashSet(pageType) ++ categories.getValues.map(_.asString().getValue)
          case None => HashSet[String](pageType)
        }

        val links = doc.get[BsonArray]("outbound").getOrElse(BsonArray()).getValues.map(_.asString().getValue).toSet

        url -> (Page(url, MSet(), tags), links)
    }} : _*)

    pages.values.foreach(p => {
      p._2.foreach(l => {
        pages.get(l) match {
          case Some(linkPage) => p._1.links += linkPage._1
          case None => Console.err.println(s"Page ${p._1.id} contains link $l not found")
        }
      })
    })

    Website(pages.map(p => p._2._1).toSet, pages.get(homepageId).get._1, pageTypes)
  }

  def loadExampleWebsite(): Website = {
    val homepage = Page("homepage", MSet(), Set(pageTypes.generic))
    val electronics = Page("electronics", MSet(), Set("electro", pageTypes.list))
    val computers = Page("computers", MSet(), Set("electro", pageTypes.product))
    val lingerie = Page("lingerie", MSet(), Set("cloth", pageTypes.product))
    val tshirts = Page("tshirts", MSet(), Set("cloth", pageTypes.product))
    val football = Page("football", MSet(), Set("sports", "football", pageTypes.product))
    val cloth = Page("cloth", MSet(), Set("cloth", pageTypes.list))
    val sports = Page("sports", MSet(), Set("sports", "football", pageTypes.list))
    val cart = Page("cart", MSet(), Set(pageTypes.cart))

    homepage.links += (electronics, cloth, sports, homepage)
    electronics.links += (computers, homepage, electronics)
    cloth.links += (tshirts, lingerie, homepage, cloth)
    sports.links += (football, homepage, sports)
    computers.links += (cart, homepage, electronics, computers)
    tshirts.links += (cart, homepage, cloth, lingerie, tshirts)
    lingerie.links += (cart, homepage, cloth, tshirts, lingerie)
    football.links += (cart, homepage, sports, football)

    Website(Set(homepage, electronics, cloth, sports, computers, tshirts, lingerie, football), homepage, pageTypes)
  }

  new Simulation {
    // System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer")

    val website = Utilities.time("load website") { loadExampleWebsite() }
    val state = new WebsiteStateVisualization(website)
    state.display

    def newUsers() {

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
        val user = AffinityUser(state.newUserId.toString, RandHelper.choose(personas).draw())
        state.users.put(user, website.homepage)
        state.visitPage(website.homepage)
        state.newUser()
      }
    }

    def userInjector() {
      if (currentTime < 100) {
        schedule(1) {
          newUsers()
          state.users.foreach { case (user: User, page: Page) =>
            val action = user.emitAction(page, website)
            action match {
              case browse: BrowseToAction =>
                val prevPage = page
                val nextPage = browse.page
                state.users.update(user, nextPage)
                state.visitPage(nextPage)
                println(s"User ${user.id} went from page ${prevPage.id} to ${nextPage.id}")
              case addToCart: AddToCartAction =>
                state.addToCart(addToCart.product)
                state.visitPage(addToCart.cartPage)
                state.visitPage(website.homepage)
                state.users.update(user, website.homepage)
                println(s"User ${user.id} added ${addToCart.product.id} to cart, back to homepage")
              case exit: ExitAction =>
                state.users.remove(user)
                println(s"User ${user.id} exited")
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

    println(state.toString)
  }

  def sleep(ms: Int = 50) {
    try { Thread.sleep(ms); } catch { case _: Throwable => }
  }
}
