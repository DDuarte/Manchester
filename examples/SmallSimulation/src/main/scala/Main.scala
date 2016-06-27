import java.util.concurrent.TimeUnit

import breeze.stats.distributions.Poisson
import com.typesafe.config.ConfigFactory
import org.mongodb.scala.MongoClient

import scala.concurrent.duration.Duration
import scala.collection.mutable.{Set => MSet}

object Main extends App {
  val config = ConfigFactory.load()

  def loadExampleWebsite(): Website = {

    val computerA = Product(
        "computera", "Computer A", "Some computer", 499.9, "€")
    val tshirtA = Product("tshirta", "T-shirt A", "Some tshirt", 9.9, "€")
    val lingerieA = Product(
        "lingeriea", "Lingerie A", "A sexy lingerie", 39.9, "€")
    val footballA = Product(
        "footballa", "Football A", "A ball, or something", 49.9, "€")

    val homepage = Page("homepage", MSet(), Set(PageTypesTags.generic), None)
    val electronics = Page(
        "electronics", MSet(), Set("electro", PageTypesTags.list), None)
    val computers = Page("computers",
                         MSet(),
                         Set("electro", PageTypesTags.product),
                         Some(computerA))
    val lingerie = Page("lingerie",
                        MSet(),
                        Set("cloth", PageTypesTags.product),
                        Some(lingerieA))
    val tshirts = Page(
        "tshirts", MSet(), Set("cloth", PageTypesTags.product), Some(tshirtA))
    val football = Page("football",
                        MSet(),
                        Set("sports", "football", PageTypesTags.product),
                        Some(footballA))
    val cloth = Page("cloth", MSet(), Set("cloth", PageTypesTags.list), None)
    val sports = Page(
        "sports", MSet(), Set("sports", "football", PageTypesTags.list), None)
    val cart = Page("cart", MSet(), Set(PageTypesTags.cart), None)

    homepage.links += (electronics, cloth, sports, homepage)
    electronics.links += (computers, homepage, electronics)
    cloth.links += (tshirts, lingerie, homepage, cloth)
    sports.links += (football, homepage, sports)
    computers.links += (cart, homepage, electronics, computers)
    tshirts.links += (cart, homepage, cloth, lingerie, tshirts)
    lingerie.links += (cart, homepage, cloth, tshirts, lingerie)
    football.links += (cart, homepage, sports, football)

    Website(Set(homepage,
                electronics,
                cloth,
                sports,
                computers,
                tshirts,
                lingerie,
                football),
            homepage)
  }

  def loadExampleProfiles(): Map[UserProfile, Double] = {
    Map(
        UserProfile(
            Map(
                "cloth" -> 0.5,
                "sports" -> 0.5
            ),
            Map(
                PageTypesTags.cart -> 0.05,
                PageTypesTags.product -> 0.5,
                PageTypesTags.list -> 0.5
            ),
            Duration(30, TimeUnit.SECONDS),
            Poisson(25),
            0.033,
            0.5
        ) -> 0.5,
        UserProfile(
            Map(
                "electro" -> 1
            ),
            Map(
                PageTypesTags.cart -> 1
            ),
            Duration(5, TimeUnit.SECONDS),
            Poisson(50),
            0.033,
            0.5
        ) -> 0.5
    )
  }

  val website = loadExampleWebsite()

  lazy val sim: WebsiteSimulation = new WebsiteSimulation(
      website,
      AffinityFactory(loadExampleProfiles()),
      DummyWebsiteAgent(),
      Some(new WebsiteStateVisualization(website)),
      10
  )

  sim.state.display()

  Utilities.time("sim run") {
    sim.run()
  }

  val mongoClient = MongoClient(config.getString("mongodb.url"))
  val database = mongoClient.getDatabase(config.getString("mongodb.db"))
  val collection = database.getCollection("simulations")

  sim.state.saveToDb(collection, sim)
  mongoClient.close()
}
