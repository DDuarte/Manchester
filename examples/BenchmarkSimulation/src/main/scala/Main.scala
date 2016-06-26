import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration
import scala.collection.mutable.{Set => MSet}

object Main extends App {

  def loadExampleWebsite(): Website = {

    val computerA = Product(
      "computera", "Computer A", "Some computer", 499.9, "€")
    val tshirtA = Product("tshirta", "T-shirt A", "Some tshirt", 9.9, "€")
    val lingerieA = Product(
      "lingeriea", "Lingerie A", "A sexy lingerie", 39.9, "€")
    val footballA = Product(
      "footballa", "Football A", "A ball, or something", 49.9, "€")

    val homepage = Page("homepage", MSet(), Set(PageTypesTags.generic), None)
    val electronics = Page(
      "electronics", MSet(), Set("electro", PageTypesTags.list), None)
    val computers = Page("computers",
      MSet(),
      Set("electro", PageTypesTags.product),
      Some(computerA))
    val lingerie = Page("lingerie",
      MSet(),
      Set("cloth", PageTypesTags.product),
      Some(lingerieA))
    val tshirts = Page(
      "tshirts", MSet(), Set("cloth", PageTypesTags.product), Some(tshirtA))
    val football = Page("football",
      MSet(),
      Set("sports", "football", PageTypesTags.product),
      Some(footballA))
    val cloth = Page("cloth", MSet(), Set("cloth", PageTypesTags.list), None)
    val sports = Page(
      "sports", MSet(), Set("sports", "football", PageTypesTags.list), None)
    val cart = Page("cart", MSet(), Set(PageTypesTags.cart), None)

    homepage.links += (electronics, cloth, sports, homepage)
    electronics.links += (computers, homepage, electronics)
    cloth.links += (tshirts, lingerie, homepage, cloth)
    sports.links += (football, homepage, sports)
    computers.links += (cart, homepage, electronics, computers)
    tshirts.links += (cart, homepage, cloth, lingerie, tshirts)
    lingerie.links += (cart, homepage, cloth, tshirts, lingerie)
    football.links += (cart, homepage, sports, football)

    Website(Set(homepage,
      electronics,
      cloth,
      sports,
      computers,
      tshirts,
      lingerie,
      football),
      homepage)
  }

  val website = loadExampleWebsite()

  // hot start, ignore run
  { new WebsiteSimulation(website, RandomFactory(100), DummyWebsiteAgent(), None, 10).run() }

  Benchmark.bm(-1, { x =>
    x.caption("sim.run()")

    for (agents <- 0 to 10000 by 1000) {
      for (steps <- 0 to 1000 by 100) {
        x.report(s"${agents.toString}a ${steps.toString}s", {
          new WebsiteSimulation(website, RandomFactory(agents), DummyWebsiteAgent(), None, steps).run()
        })
      }
    }

    x.separator()
    x.avg()
    x.total()
  })
}
