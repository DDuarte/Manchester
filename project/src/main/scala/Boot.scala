import MongoHelpers._
import com.typesafe.config.ConfigFactory
import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.{BsonArray, BsonString}
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Sorts._

import scala.collection.JavaConversions._
import scala.collection.mutable.{Set => MSet}

object Boot extends App {
  val config = ConfigFactory.load()

  def loadMongoWebsite(url: String, dbName: String, collectionName: String): Website = {
    val mongoClient = MongoClient(url)
    val database = mongoClient.getDatabase(dbName)
    val collection = database.getCollection(collectionName)

    var homepageId: String = null

    val pages = Map(collection
      .find()
      .projection(include("url", "type", "category", "outbound"))
      .sort(ascending("_id"))
      .results()
      .map { doc => {
        val url = doc.get[BsonString]("url").get.getValue

        if (homepageId == null) homepageId = url

        val pageType = doc.get[BsonString]("type").map(_.asString().getValue) match {
          case Some(t) if t == config.getString("types.list") => PageTypesTags.list
          case Some(t) if t == config.getString("types.product") => PageTypesTags.product
          case Some(t) if t == config.getString("types.cart") => PageTypesTags.cart
          case Some(t) if t == config.getString("types.generic") => PageTypesTags.generic
          case Some(t) =>
            Console.err.println(s"Page $url has unknown type $t")
            PageTypesTags.generic
          case None =>
            Console.err.println(s"Page $url has no type")
            PageTypesTags.generic
        }

        val tags: Set[String] = doc.get[BsonArray]("tags").getOrElse(BsonArray()).getValues.map(_.asString().getValue).toSet + pageType
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

    Website(pages.map(p => p._2._1).toSet, pages.get(homepageId).get._1)
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

  val website = Utilities.time("load website") {
    loadMongoWebsite(
      config.getString("mongodb.url"),
      config.getString("mongodb.db"),
      config.getString("mongodb.collection"))
  }

  var sim = new WebsiteSimulation(website)
  sim.state.display
  Utilities.time("sim run") {
    sim.run()
  }
  println(sim.state.toString)
}
