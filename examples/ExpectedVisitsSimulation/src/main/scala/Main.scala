import java.util.concurrent.TimeUnit

import breeze.stats.distributions.{Poisson, Rand}
import com.typesafe.config.ConfigFactory
import org.mongodb.scala.MongoClient

import scala.concurrent.duration.Duration
import scala.collection.mutable.{Set => MSet}
import scala.concurrent.Await

case class MyFactory(newUsers: Int) extends UserFactory[MyUser] {

  def getNewUserId: String = java.util.UUID.randomUUID().toString

  val users: Iterator[List[MyUser]] = {
    Iterator.continually(List.fill(newUsers)(MyUser(getNewUserId)))
  }
}

case class MyUser(id: String) extends User {
  override def emitAction(currentPage: Page, website: Website): Action = {

    if (currentPage.tags.contains(PageTypesTags.cart)) {
      ExitAction()
    } else if (currentPage.tags.contains(PageTypesTags.product)) {
      val cartPage =
        currentPage.links.find(l => l.tags.contains(PageTypesTags.cart)).get
      AddToCartAction(currentPage.product.get, cartPage)
    } else {
      val nextPage = Rand.choose(currentPage.links).draw()
      BrowseToAction(nextPage)
    }
  }
}

object Main extends App {
  val config = ConfigFactory.load()

  def loadExampleWebsite(): Website = {
    val product1 = Product("product1", "product1", "product1", 10, "€")
    val product2 = Product("product2", "product2", "product2", 10, "€")
    val product3 = Product("product3", "product3", "product3", 10, "€")
    val product4 = Product("product4", "product4", "product4", 10, "€")
    val product5 = Product("product5", "product5", "product5", 10, "€")

    val homepage = Page("homepage", MSet(), Set(PageTypesTags.generic), None)

    val page1 = Page("page1", MSet(), Set(PageTypesTags.product, "cat1"), Some(product1))
    val page2 = Page("page2", MSet(), Set(PageTypesTags.product, "cat2"), Some(product2))
    val page3 = Page("page3", MSet(), Set(PageTypesTags.product, "cat3"), Some(product3))
    val page4 = Page("page4", MSet(), Set(PageTypesTags.product, "cat4"), Some(product4))
    val page5 = Page("page5", MSet(), Set(PageTypesTags.product, "cat5"), Some(product5))

    val cart = Page("cart", MSet(), Set(PageTypesTags.cart), None)

    homepage.links +=(page1, page2, page3, page4, page5)
    page1.links += cart
    page2.links += cart
    page3.links += cart
    page4.links += cart
    page5.links += cart
    cart.links += homepage

    Website(Set(homepage,
      page1,
      page2,
      page3,
      page4,
      page5, cart),
      homepage)
  }

  val website = loadExampleWebsite()

  lazy val sim: WebsiteSimulation = new WebsiteSimulation(
    website,
    MyFactory(100),
    DummyWebsiteAgent(),
    None,
    1000
  )

  Utilities.time("sim run") {
    sim.run()
  }

  val mongoClient = MongoClient(config.getString("mongodb.url"))
  val database = mongoClient.getDatabase(config.getString("mongodb.db"))
  val collection = database.getCollection("simulations")

  Await.result(sim.state.saveToDb(collection, sim), Duration(20000, TimeUnit.SECONDS))
  mongoClient.close()
}
