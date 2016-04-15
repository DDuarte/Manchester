import breeze.stats.distributions.{DiscreteDistr, Rand}

import scala.concurrent.duration.Duration

trait User {
  def emitAction(currentPage: Page, website: Website): Action
  def id: String
}

case class RandomUser(id: String) extends User {
  override def emitAction(currentPage: Page, website: Website): Action = {
    if (Rand.randInt(3).draw() == 2 /* 33.3% */ || currentPage.links.isEmpty)
      ExitAction()
    else if (Rand.randInt(101).draw() <= 5 /* 5% */ && currentPage.tags.contains(PageTypesTags.product)) {
      val cartPage = currentPage.links.find(l => l.tags.contains(PageTypesTags.cart)).get
      AddToCartAction(currentPage, cartPage)
    } else {
      val nextPage = Rand.choose(currentPage.links).draw()
      BrowseToAction(nextPage)
    }
  }
}

case class AffinityUser(id: String, profile: UserProfile) extends User {
  override def emitAction(currentPage: Page, website: Website): Action = {
    if ((Rand.randInt(101).draw() < profile.exitProb * 100) || currentPage.links.isEmpty)
      ExitAction()
    else if ((Rand.randInt(101).draw() <= profile.addToCartProb * 100) && currentPage.tags.contains(PageTypesTags.product)) {
      // assumed that a product page links to a cart page
      val cartPage = currentPage.links.find(l => l.tags.contains(PageTypesTags.cart)).get
      AddToCartAction(currentPage, cartPage)
    } else {
      val affSelected = RandHelper.choose(profile.affinities).draw()

      val links = currentPage.links.filter(p => p.tags.contains(affSelected) &&
        (p.tags.contains(PageTypesTags.product) || p.tags.contains(PageTypesTags.list)))

      val nextPage = if (links.isEmpty)
        Rand.choose(currentPage.links).draw()
      else
        Rand.choose(links).draw()

      BrowseToAction(nextPage)
    }
  }
}

case class UserProfile(
  affinities:             Map[String, Double],
  pageTypeWeights:        Map[String, Double],
  averageSessionDuration: Duration,
  arrivalDistribution:    DiscreteDistr[Int],
  addToCartProb:          Double,
  exitProb:               Double
)
