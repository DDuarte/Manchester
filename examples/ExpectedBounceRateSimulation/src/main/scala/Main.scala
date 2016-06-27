import java.util.concurrent.TimeUnit

import breeze.stats.distributions.{Poisson, Rand}
import com.typesafe.config.ConfigFactory
import org.mongodb.scala.MongoClient

import scala.concurrent.duration.Duration
import scala.collection.mutable.{Set => MSet}
import scala.concurrent.Await

object Main extends App {
  val config = ConfigFactory.load()

  def loadExampleWebsite(): Website = {
    val homepage = Page("homepage", MSet(), Set(PageTypesTags.generic), None)

    Website(Set(homepage),
      homepage)
  }

  val website = loadExampleWebsite()

  lazy val sim: WebsiteSimulation = new WebsiteSimulation(
    website,
    RandomFactory(100),
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
