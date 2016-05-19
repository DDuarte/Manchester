import java.lang.reflect.ParameterizedType
import java.text.DecimalFormat
import java.util.{Calendar, Date}

import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala._

import scala.collection.immutable.ListMap
import scala.collection.mutable.{HashMap => MHashMap, Map => MMap, Set => MSet}
import scala.concurrent.Future

case class Website(
    pages: Set[Page],
    homepage: Page
)

case class Product(
    id: String,
    name: String,
    description: String,
    price: Double,
    currency: String
)

class Page(val id: String,
           val links: MSet[Page],
           val tags: Set[String],
           val product: Option[Product]) {
  def canEqual(other: Any): Boolean = other.isInstanceOf[Page]

  override def equals(other: Any): Boolean = other match {
    case that: Page =>
      (that canEqual this) && id == that.id
    case _ => false
  }

  override def hashCode(): Int = {
    id.hashCode
  }

  // these methods have to be overriden because Page may have links referencing back to itself
  // if Page is a case class, the default hashCode implementation will stack overflow
}

object Page {
  def apply(id: String,
            links: MSet[Page],
            tags: Set[String],
            product: Option[Product]) = {
    assert(!tags.contains(PageTypesTags.product) || product.isDefined)
    new Page(id, links, tags, product)
  }
}

class WebsiteState {

  protected val visits = MHashMap[Page, Long]()
  protected val visitsPerUser = MHashMap[User, Long]()
  protected val visitsPerCategory = MMap[String, MMap[String, Long]]()
    .withDefaultValue(MMap[String, Long]().withDefaultValue(0l))
  protected val purchases = MHashMap[Product, Long]()
  protected var uniqueUserCount = 0l
  val users = MHashMap[User, Page]()

  protected var singleSessionCount = 0l
  protected var sessionCount = 0l
  protected def bounceRate = singleSessionCount.toDouble / sessionCount

  def display(): Unit = println(toString)

  def visitPage(user: User, page: Page) {
    users.put(user, page)
    visits += (page -> (visits.getOrElse(page, 0l) + 1l))
    visitsPerUser += (user -> (visitsPerUser.getOrElse(user, 0l) + 1l))

    val tagList = page.tags.filter(t => !t.startsWith("_"))

    if (tagList.nonEmpty) {
      val firstTag = tagList.head
      val otherTags = tagList.tail.mkString(",")

      visitsPerCategory.getOrElseUpdate(firstTag, MMap[String, Long]()) +=
        (otherTags -> (visitsPerCategory
                .get(firstTag)
                .get
                .getOrElseUpdate(otherTags, 0l) + 1l))
    }
  }

  def addToCart(product: Product): Unit = {
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

  var simulationStartTime: Option[Date] = None
  var simulationEndTime: Option[Date] = None

  def startSimulation() = {
    simulationStartTime = Some(Calendar.getInstance().getTime)
  }

  def finishSimulation() = {
    simulationEndTime = Some(Calendar.getInstance().getTime)
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
        sb ++= f"${product.name}%50s $count%5d ${decFormat.format(
            count * product.price)}%10s${product.currency}\n"
    }

    sb.toString()
  }

  def toJson(sim: WebsiteSimulation): String = {
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
            s._1.replace('.', '_') -> s._2.map { ss =>
              ss._1.replace('.', '_') -> ss._2
            }.toMap
          }.toMap) ~
      ("purchases" ->
          ListMap(purchases.toSeq.sortWith(_._2 > _._2).map {
            case (product, count) =>
              product.name.replace('.', '_') ->
              ("count" -> count) ~
              ("totalPrice" -> product.price * count) ~
              ("currency" -> product.currency)
          }: _*)) ~
      ("userFactoryName" -> sim.userFactory.getClass.getTypeName) ~
      ("userAgentName" -> sim.userFactory.getClass.getGenericInterfaces.head
            .asInstanceOf[ParameterizedType]
            .getActualTypeArguments
            .head
            .getTypeName) ~
      ("websiteAgentName" -> sim.websiteAgent.getClass.getTypeName) ~
      ("simulationStartTime" -> simulationStartTime.get.toString) ~
      ("simulationEndTime" -> simulationEndTime.get.toString)

    pretty(render(json))
  }

  def saveToDb(collection: MongoCollection[Document],
               sim: WebsiteSimulation): Future[Seq[Completed]] = {
    // TODO: atm this only supports mongodb backend
    collection.insertOne(Document(toJson(sim))).toFuture()
  }
}

object PageTypesTags {
  val list = "_productList"
  val product = "_product"
  val cart = "_cart"
  val generic = "_generic"
}
