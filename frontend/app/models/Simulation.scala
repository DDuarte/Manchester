package models

import javax.inject.Inject

import akka.actor.ActorRef
import akka.stream.scaladsl.Source
import akka.util.Timeout
import com.github.jeroenr.bson.BsonDocument
import com.github.jeroenr.bson.BsonDsl._
import com.github.jeroenr.bson.Implicits.BsonValueObjectId
import com.github.jeroenr.bson.element.BsonObjectId
import com.github.jeroenr.bson.util.Converters
import helpers.BsonDocumentHelper._
import play.api.libs.json.{JsResult, JsSuccess, Json}
import play.api.modules.tepkinmongo.TepkinMongoApi

import scala.concurrent.Future
import scala.concurrent.duration._

case class Simulation(
  _id:               String,
  name:              String,
  uniqueUsers:       Int,
  bounceRate:        Double,
  visits:            Map[String, Int],
  visitsPerCategory: Map[String, Map[String, Int]],
  purchases:         Map[String, Int]
)

object Simulation {
  implicit val simulationFormatter = Json.format[Simulation]
  def apply(bson: BsonDocument): JsResult[Simulation] = {
    Json.fromJson[Simulation](bson)
  }

  def toBsonDocument(simulation: Simulation): BsonDocument =
    ("_id" := simulation._id) ~
      ("name" := simulation.name)
}

class SimulationRepo @Inject() (tepkinMongoApi: TepkinMongoApi) {
  implicit val ec = tepkinMongoApi.client.ec
  implicit val timeout: Timeout = 5.seconds

  val simulations = tepkinMongoApi.client("kugsha")("simulations")

  def create(name: String): Future[Boolean] = {
    val simulation = "name" := name
    simulations.insert(simulation).map(_.ok)
  }

  def all: Source[List[Simulation], ActorRef] = {
    simulations.find(new BsonDocument()).map(l => l.map(Simulation(_)).collect {
      case JsSuccess(p, _) => p
    })
  }

  def insert(ps: List[Simulation]) =
    simulations.insert(ps.map(Simulation.toBsonDocument))

  private def stringToObjectId(id: String) =
    BsonObjectId("_id", BsonValueObjectId(Converters.str2Hex(id)))

  def findById(id: String): Future[Option[Simulation]] = {
    simulations.findOne(stringToObjectId(id)).map(_.flatMap(Simulation(_).asOpt))
  }
}
