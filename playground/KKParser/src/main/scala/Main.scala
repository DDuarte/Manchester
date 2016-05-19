import scala.io.Source
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.collection.immutable.HashSet
import scala.collection.mutable

object Main {
  def main(args: Array[String]) {

    implicit val formats = DefaultFormats

    // val path = "E:\\dump-20151020-20151028.log"
    //val path = "E:\\dump-20151001-20151010.log"
    val path = "E:\\dump-20151010-20151020.log"

    val events = mutable.HashSet[String]()

    for (line <- Source.fromFile(path, "UTF-8").getLines()) {
      val json = parse(line)
      val event = (json \ "meta" \ "type").extractOpt[String]
      val pageType = (json \ "uri" \ "query" \ "pageType").extractOpt[String]
      val title = (json \ "uri" \ "query" \ "title").extractOpt[String]
      val t = (json \ "uri" \ "query" \ "type").extractOpt[String]
      val clientId = (json \ "uri" \ "query" \ "clientId").extractOpt[String]
      val location = (json \ "uri" \ "query" \ "location").extractOpt[String]
      val uid = (json \ "meta" \ "uid").extractOpt[String]
      val prodName =
        (json \ "uri" \ "query" \ "product" \ "name").extractOpt[String]

      clientId match {
        case Some("kuantokusta") => {

            if (t.getOrElse("") == "pageView" &&
                events.add(pageType.getOrElse(""))) {
              println(pageType + " - " + line)
            }

            /* if (uid.isDefined && event.isDefined && (title.isDefined || prodName.isDefined)) {

            val name = if (prodName.isDefined) {
              prodName.map(_.replace(",", ";")).get
            } else {
              title.map(_.stripSuffix(" - Comparador de preços e guia de compras online").replace(",", ";")).get
            }

            println(uid.get + "," + name + "," + event.get)
          }*/
          }
        case _ =>
      }
    }
  }
}
