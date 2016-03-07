import MongoHelpers._
import breeze.stats.distributions.Poisson
import com.typesafe.config.ConfigFactory
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.{BsonArray, BsonString}
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._

import scala.collection.JavaConversions._
import scala.collection.immutable.HashSet
import scala.collection.mutable.{HashMap => MHashMap, Set => MSet}

object Boot extends App {
  val config = ConfigFactory.load()

  val pageTypes = WebsitePageTypes(
    config.getString("types.list"),
    config.getString("types.product"),
    config.getString("types.cart"),
    config.getString("types.generic")
  )

  def loadMongoWebsite(url: String, dbName: String, collectionName: String): Website = {
    val mongoClient = MongoClient(url)
    val database = mongoClient.getDatabase(dbName)
    val collection = database.getCollection(collectionName)

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
          case Some(categories) => HashSet(pageType) ++ categories.getValues.map(_.asString().getValue)
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

    val website = Utilities.time("load website") {
      loadMongoWebsite(
      config.getString("mongodb.url"),
      config.getString("mongodb.db"),
      config.getString("mongodb.collection"))
    }

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
