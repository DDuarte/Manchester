import java.util.concurrent.TimeUnit

import breeze.stats.distributions.Poisson
import com.typesafe.config.ConfigFactory
import org.bson.BsonDocument
import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.mongodb.scala.bson.{BsonArray, BsonInt64, _}
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._

import scala.concurrent.duration.Duration
import scala.collection.mutable.{Set => MSet}
import scala.collection.JavaConversions._
import MongoHelpers._

object Main extends App {
  val config = ConfigFactory.load()

  val mongoClient = MongoClient(config.getString("mongodb.url"))
  val database = mongoClient.getDatabase(config.getString("mongodb.db"))

  def loadMongoWebsite(database: MongoDatabase, collectionName: String): Website = {
    val collection = database.getCollection(collectionName)

    var homepageId: String = null

    val pages = Map(collection
      .find()
      .projection(include(
        config.getString("mongodb.collections.pages.id"),
        config.getString("mongodb.collections.pages.type"),
        config.getString("mongodb.collections.pages.categories"),
        config.getString("mongodb.collections.pages.links"),
        "productName", "productDescription", "price", "dynamicCount"
      ))
      .sort(ascending("_id"))
      // .limit(1000)
      .results()
      .map { doc =>
        {
          val id = doc.get[BsonString](config.getString("mongodb.collections.pages.id")).get.getValue

          if (homepageId == null) homepageId = id

          val pageType = doc.get[BsonString](config.getString("mongodb.collections.pages.type"))
            .map(_.asString().getValue) match {
              case Some(t) if t == config.getString("types.list") => PageTypesTags.list
              case Some(t) if t == config.getString("types.product") => PageTypesTags.product
              case Some(t) if t == config.getString("types.cart") => PageTypesTags.cart
              case Some(t) if t == config.getString("types.generic") => PageTypesTags.generic
              case Some(t) =>
                Console.err.println(s"Page $id has unknown type $t")
                PageTypesTags.generic
              case None =>
                Console.err.println(s"Page $id has no type")
                PageTypesTags.generic
            }

          val product = if (pageType == PageTypesTags.product) {

            val productName = doc.get[BsonString]("productName").get.getValue
            val productDescription = doc.get[BsonString]("productDescription")
              .getOrElse(BsonString("No description")).getValue

            val pricePattern = "([0-9]+\\.?,?[0-9]+) ?(.+)".r
            val pricePattern(price, currency) = doc.get[BsonString]("price").get.getValue

            Some(Product(id, productName, productDescription, price.replace(',', '.').toDouble, currency))
          } else
            None

          /*val optionalCart = doc.get[Boolean]("cart") match {
            case Some(true) => Some(PageTypesTags.cart)
            case Some(false) => None
            case None => None
          }*/

          val tags: Set[String] = doc.get[BsonArray](config.getString("mongodb.collections.pages.categories"))
            .getOrElse(BsonArray()).getValues.map(_.asString().getValue).toSet + pageType
          val links = doc.get[BsonArray](config.getString("mongodb.collections.pages.links"))
            .getOrElse(BsonArray()).getValues.map(_.asString().getValue).toSet

          id -> (Page(id, MSet(), tags, product), links)
        }
      }: _*)

    pages.values.foreach(p => {
      p._2.foreach(l => {
        pages.get(l) match {
          case Some(linkPage) => p._1.links += linkPage._1
          case None => Console.err.println(s"Page ${p._1.id} contains link $l not found")
        }
      })
    })

    Website(pages.map(p => p._2._1).toSet, pages.get(homepageId).get._1)
  }

  def loadMongoProfiles(database: MongoDatabase, collectionName: String): Map[UserProfile, Double] = {
    val collection = database.getCollection(collectionName)

    Map(collection
      .find()
      .projection(include(
        config.getString("mongodb.collections.profiles.flow"),
        config.getString("mongodb.collections.profiles.affinities"),
        config.getString("mongodb.collections.profiles.pageWeights"),
        config.getString("mongodb.collections.profiles.avgDuration")
      ))
      .sort(ascending("_id"))
      .results()
      .map { doc =>
        {
          val affinities: Map[String, Double] = Map(doc.get[BsonDocument](config.getString("mongodb.collections.profiles.affinities"))
            .getOrElse(new BsonDocument()).entrySet.map { e => { e.getKey -> e.getValue.asNumber().doubleValue() } }.toSeq: _*)

          val pageWeights: Map[String, Double] = Map(doc.get[BsonDocument](config.getString("mongodb.collections.profiles.pageWeights"))
            .getOrElse(new BsonDocument()).entrySet.map { e => { e.getKey -> e.getValue.asNumber().doubleValue() } }.toSeq: _*)

          val duration = Duration(doc.get[BsonInt64](config.getString("mongodb.collections.profiles.avgDuration"))
            .getOrElse(BsonInt64(0l)).longValue(), TimeUnit.SECONDS)

          UserProfile(affinities, pageWeights, duration, Poisson(250), 0.05, 0.15 /* TODO: hardcoded */ ) -> 1.0 /* TODO: hardcoded */
        }
      }: _*)
  }

  val website = Utilities.time("load website") {
    loadMongoWebsite(
      database,
      config.getString("mongodb.collections.pages.name")
    )
  }

  val profiles = loadMongoProfiles(
    database,
    config.getString("mongodb.collections.profiles.name")
  )

  var sim = new WebsiteSimulation(website, AffinityFactory(profiles), DummyWebsiteAgent())

  Utilities.time("sim run") {
    sim.run()
  }

  println(sim.state)

  val collection = database.getCollection("simulations")

  sim.state.saveToDb(collection, sim)

  mongoClient.close()
}
