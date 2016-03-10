import java.util.concurrent.TimeUnit

import MongoHelpers._
import breeze.stats.distributions.Poisson
import com.typesafe.config.ConfigFactory
import org.bson.{BsonValue, BsonDouble, BsonDocument}
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.{BsonInt64, BsonArray, BsonString}
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._

import scala.collection.JavaConversions._
import scala.collection.mutable.{Set => MSet}
import scala.concurrent.duration.Duration

object Boot extends App {
  val config = ConfigFactory.load()

  def loadMongoWebsite(url: String, dbName: String, collectionName: String): Website = {
    val mongoClient = MongoClient(url)
    val database = mongoClient.getDatabase(dbName)
    val collection = database.getCollection(collectionName)

    var homepageId: String = null

    val pages = Map(collection
      .find()
      .projection(include(
        config.getString("mongodb.collections.pages.id"),
        config.getString("mongodb.collections.pages.type"),
        config.getString("mongodb.collections.pages.categories"),
        config.getString("mongodb.collections.pages.links")))
      .sort(ascending("_id"))
      .results()
      .map { doc => {
        val id = doc.get[BsonString](config.getString("mongodb.collections.pages.id")).get.getValue

        if (homepageId == null) homepageId = id

        val pageType = doc.get[BsonString](config.getString("mongodb.collections.pages.type")).map(_.asString().getValue) match {
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

        val tags: Set[String] = doc.get[BsonArray](config.getString("mongodb.collections.pages.categories"))
          .getOrElse(BsonArray()).getValues.map(_.asString().getValue).toSet + pageType
        val links = doc.get[BsonArray](config.getString("mongodb.collections.pages.links"))
          .getOrElse(BsonArray()).getValues.map(_.asString().getValue).toSet

        id -> (Page(id, MSet(), tags), links)
    }} : _*)

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

  def loadMongoProfiles(url: String, dbName: String, collectionName: String) : Map[UserProfile, Double] = {
    val mongoClient = MongoClient(url)
    val database = mongoClient.getDatabase(dbName)
    val collection = database.getCollection(collectionName)

    Map(collection
      .find()
      .projection(include(
        config.getString("mongodb.collections.profiles.flow"),
        config.getString("mongodb.collections.profiles.affinities"),
        config.getString("mongodb.collections.profiles.pageWeights"),
        config.getString("mongodb.collections.profiles.avgDuration")))
      .sort(ascending("_id"))
      .results()
      .map { doc => {
        val affinities: Map[String, Double] = Map(doc.get[BsonDocument](config.getString("mongodb.collections.profiles.affinities"))
          .getOrElse(new BsonDocument()).entrySet.map { e => { e.getKey -> e.getValue.asDouble().getValue }}.toSeq : _*)

        val pageWeights: Map[String, Double] = Map(doc.get[BsonDocument](config.getString("mongodb.collections.profiles.pageWeights"))
          .getOrElse(new BsonDocument()).entrySet.map { e => { e.getKey -> e.getValue.asDouble().getValue }}.toSeq : _*)

        val duration = Duration(doc.get[BsonInt64](config.getString("mongodb.collections.profiles.avgDuration"))
          .getOrElse(BsonInt64(0l)).longValue(), TimeUnit.SECONDS)

        UserProfile(affinities, pageWeights, duration, Poisson(25) /* TODO: hardcoded */) -> 1.0 /* TODO: hardcoded */
      }} : _*)
  }

  def loadExampleWebsite(): Website = {
    val homepage = Page("homepage", MSet(), Set(PageTypesTags.generic))
    val electronics = Page("electronics", MSet(), Set("electro", PageTypesTags.list))
    val computers = Page("computers", MSet(), Set("electro", PageTypesTags.product))
    val lingerie = Page("lingerie", MSet(), Set("cloth", PageTypesTags.product))
    val tshirts = Page("tshirts", MSet(), Set("cloth", PageTypesTags.product))
    val football = Page("football", MSet(), Set("sports", "football", PageTypesTags.product))
    val cloth = Page("cloth", MSet(), Set("cloth", PageTypesTags.list))
    val sports = Page("sports", MSet(), Set("sports", "football", PageTypesTags.list))
    val cart = Page("cart", MSet(), Set(PageTypesTags.cart))

    homepage.links += (electronics, cloth, sports, homepage)
    electronics.links += (computers, homepage, electronics)
    cloth.links += (tshirts, lingerie, homepage, cloth)
    sports.links += (football, homepage, sports)
    computers.links += (cart, homepage, electronics, computers)
    tshirts.links += (cart, homepage, cloth, lingerie, tshirts)
    lingerie.links += (cart, homepage, cloth, tshirts, lingerie)
    football.links += (cart, homepage, sports, football)

    Website(Set(homepage, electronics, cloth, sports, computers, tshirts, lingerie, football), homepage)
  }

  def loadExampleProfiles() : Map[UserProfile, Double] = {
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
        Poisson(25)
      ) -> 0.5,
      UserProfile(
        Map(
          "electro" -> 1
        ),
        Map(
          PageTypesTags.cart ->1
        ),
        Duration(5, TimeUnit.SECONDS),
        Poisson(50)
      ) -> 0.5
    )
  }

  val website = Utilities.time("load website") {
    loadMongoWebsite(
      config.getString("mongodb.url"),
      config.getString("mongodb.db"),
      config.getString("mongodb.collections.pages.name"))
  }

  val profiles = loadMongoProfiles(
    config.getString("mongodb.url"),
    config.getString("mongodb.db"),
    config.getString("mongodb.collections.profiles.name"))

  var sim = new WebsiteSimulation(website, profiles)
  // sim.state.display
  Utilities.time("sim run") {
    sim.run()
  }
  println(sim.state.toString)
}
