import java.util.concurrent.TimeUnit

import breeze.stats.distributions.Poisson

import scala.concurrent.duration.Duration
import scala.collection.mutable.{Set => MSet}

object Main extends App {
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

  def loadExampleProfiles(): Map[UserProfile, Double] = {
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
        Poisson(25), 0.033, 0.5
      ) -> 0.5,
      UserProfile(
        Map(
          "electro" -> 1
        ),
        Map(
          PageTypesTags.cart -> 1
        ),
        Duration(5, TimeUnit.SECONDS),
        Poisson(50), 0.033, 0.5
      ) -> 0.5
    )
  }

  val website = loadExampleWebsite()

  val sim = new WebsiteSimulation(
    website,
    AffinityFactory(loadExampleProfiles()),
    DummyWebsiteAgent(),
    Some(new WebsiteStateVisualization(website))
  )

  sim.state.display()

  Utilities.time("sim run") {
    sim.run()
  }

  println(sim.state)
  sim.state.saveToDb
}
