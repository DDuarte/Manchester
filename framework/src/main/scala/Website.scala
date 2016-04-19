import java.text.DecimalFormat

import com.typesafe.config.ConfigFactory
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.{Completed, MongoClient}

import scala.collection.immutable.ListMap
import scala.collection.mutable.{HashMap => MHashMap, Map => MMap, Set => MSet}
import scala.concurrent.Future

case class Website(pages: Set[Page], homepage: Page)

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

class WebsiteState(website: Website) {
  protected val visits = MHashMap[Page, Long]()
  protected val visitsPerUser = MHashMap[User, Long]()
  protected val visitsPerCategory =
    MMap[String, MMap[String, Long]]().withDefaultValue(MMap[String, Long]().withDefaultValue(0l))
  protected val purchases = MHashMap[Page /* Product */ , Long]()
  protected var uniqueUserCount = 0l
  val users = MHashMap[User, Page]()

  protected var singleSessionCount = 0l
  protected var sessionCount = 0l
  protected def bounceRate = singleSessionCount.toDouble / sessionCount

  def display(): Unit = ???

  def visitPage(user: User, page: Page) {
    users.put(user, page)
    visits += (page -> (visits.getOrElse(page, 0l) + 1l))
    visitsPerUser += (user -> (visitsPerUser.getOrElse(user, 0l) + 1l))

    val tagList = page.tags.filter(t => !t.startsWith("_"))

    if (tagList.nonEmpty) {
      val firstTag = tagList.head
      val otherTags = tagList.drop(1).mkString(",")

      visitsPerCategory.getOrElseUpdate(firstTag, MMap[String, Long]()) +=
        (otherTags -> (visitsPerCategory.get(firstTag).get.getOrElseUpdate(otherTags, 0l) + 1l))
    }
  }

  def addToCart(product: Page): Unit = {
    purchases += (product -> (purchases.getOrElse(product, 0l) + 1l))
  }

  def exit(user: User): Unit = {
    users.remove(user)

    if (visitsPerUser.getOrElse(user, 0l) == 1) {
      singleSessionCount += 1
    }
  }

  def newUser() = {
    uniqueUserCount += 1
    sessionCount += 1
  }

  override def toString: String = {
    val sb = new StringBuilder()

    sb ++= "--- Website statistics ---\n"

    sb ++= "\n- Unique users: " ++= uniqueUserCount.toString ++= "\n"

    val decFormat = new DecimalFormat("#.###")
    sb ++= "\n- Bounce rate: " ++= decFormat.format(bounceRate * 100) ++= "%\n"

    sb ++= "\n- Visits:\n"

    visits.toSeq.sortWith(_._2 > _._2).foreach {
      case (page, count) =>
        sb ++= f"${page.id}%15s $count%5d\n"
    }

    /*sb ++= "\n- Visits per user:\n"

    visitsPerUser.toSeq.sortWith(_._2 > _._2).foreach {
      case (user, count) =>
        sb ++= f"${user.id}%15s $count%5d\n"
    }*/

    sb ++= "\n- Purchases:\n"

    purchases.toSeq.sortWith(_._2 > _._2).foreach {
      case (product, count) =>
        sb ++= f"${product.id}%15s $count%5d\n"
    }

    sb.toString()
  }

  def toJson: String = {
    import org.json4s.JsonDSL._
    import org.json4s.native.JsonMethods._

    val json =
      ("name" -> "Simulation") ~
        ("uniqueUsers" -> uniqueUserCount) ~
        ("bounceRate" -> bounceRate) ~
        ("visits" ->
          ListMap(visits.toSeq.sortWith(_._2 > _._2).map {
            case (page, count) =>
              page.id.replace('.', '_') -> count
          }: _*)) ~
          ("visitsPerCategory" -> visitsPerCategory.map { s =>
            s._1.replace('.', '_') -> s._2.map { ss => ss._1.replace('.', '_') -> ss._2 }.toMap
          }.toMap) ~
          ("purchases" ->
            ListMap(purchases.toSeq.sortWith(_._2 > _._2).map {
              case (product, count) =>
                product.id.replace('.', '_') -> count
            }: _*))

    pretty(render(json))
  }

  def saveToDb: Future[Seq[Completed]] = {
    // TODO: atm this only supports mongodb backend

    val config = ConfigFactory.load()

    val mongoClient = MongoClient(config.getString("mongodb.url"))
    val database = mongoClient.getDatabase(config.getString("mongodb.db"))
    val collection = database.getCollection("simulations")

    collection.insertOne(Document(toJson)).toFuture()
  }
}

object PageTypesTags {
  val list = "_productList"
  val product = "_product"
  val cart = "_cart"
  val generic = "_generic"
}
