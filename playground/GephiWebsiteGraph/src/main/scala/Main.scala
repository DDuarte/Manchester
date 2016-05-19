import java.io.{BufferedWriter, File, FileWriter}

import MongoHelpers._

import org.mongodb.scala.MongoClient
import org.mongodb.scala.bson.{BsonArray, _}
import org.mongodb.scala.model.Projections._

import scala.collection.JavaConversions._

object Main {
  def main(args: Array[String]): Unit = {
    val mongoClient = MongoClient("mongodb://localhost")
    val database = mongoClient.getDatabase("kugsha")
    val collection = database.getCollection("clickfiel-pages")

    val file = new File("clickfiel-pages.csv")
    val bw = new BufferedWriter(new FileWriter(file))

    collection
      .find()
      .projection(include("url", "outbound"))
      .results()
      .foreach { doc =>
        {
          val id = doc.get[BsonString]("url").get.getValue

          val links = doc
            .get[BsonArray]("outbound")
            .getOrElse(BsonArray())
            .getValues
            .map(_.asString().getValue)
            .toSet

          bw.write(id)
          links.foreach(link => {
            bw.write(';')
            bw.write(link)
          })

          bw.write('\n')
        }
      }

    bw.close()
  }
}
