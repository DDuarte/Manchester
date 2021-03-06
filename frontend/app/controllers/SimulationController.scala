package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.ByteString
import models.{Simulation, SimulationRepo}
import play.api.http.HttpEntity.Streamed
import play.api.http.MimeTypes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._

// @Singleton
class SimulationController @Inject() (simulationRepo: SimulationRepo)
    extends Controller {

  implicit val mt: MessageFlowTransformer[Simulation, String] = {
    MessageFlowTransformer.stringMessageFlowTransformer.map { s =>
      Json.fromJson[Simulation](Json.parse(s)) match {
        case JsSuccess(simulation, _) => simulation
        case JsError(_) =>
          Simulation(
            "error",
            "error",
            0,
            0.0,
            Map(),
            Map(),
            Map(),
            "",
            "",
            "",
            "",
            ""
          )
      }
    }
  }

  implicit val system = ActorSystem("Sys")
  implicit val materializer = ActorMaterializer()

  def simulationsVisitsPerCategoryApi(id: String) = Action.async {
    for {
      Some(simulation) <- simulationRepo.findById(id)
      obj: Map[String, List[Map[String, JsValue]]] = Map(
        ("series", simulation.visitsPerCategory.map { s =>
          Map(
            ("name", JsString(s._1)),
            ("drilldown", JsString(s._1)),
            ("y", JsNumber(s._2.values.sum))
          )
        }.toList),
        ("drilldown", simulation.visitsPerCategory.map { s =>
          Map(
            ("name", JsString(s._1)),
            ("id", JsString(s._1)),
            ("data", JsArray(s._2.map { ss =>
              JsArray(List(JsString(ss._1), JsNumber(ss._2)))
            }.toList))
          )
        }.toList)
      )
    } yield Ok(Json.toJson(obj))
  }

  def simulations(id: String) = Action.async {
    for {
      Some(simulation) <- simulationRepo.findById(id)
      sim = simulation.copy(
        purchases = simulation.purchases.map(s =>
          (s._1, s._2.copy(currency = "&euro;")))
      )
    } yield Ok(views.html.simulation(sim))
  }

  def compare(a: String, b: String) = Action.async {
    for {
      Some(simulationA) <- simulationRepo.findById(a)
      simA = simulationA.copy(
        purchases = simulationA.purchases.map(s =>
          (s._1, s._2.copy(currency = "&euro;")))
      )
      Some(simulationB) <- simulationRepo.findById(b)
      simB = simulationB.copy(
        purchases = simulationB.purchases.map(s =>
          (s._1, s._2.copy(currency = "&euro;")))
      )
    } yield Ok(views.html.compare(simA, simB))
  }

  def index = Action.async {
    for {
      s <- simulationRepo.all.runFold(List.empty[Simulation])(_ ++ _)
    } yield Ok(views.html.simulations(s))
  }
}
