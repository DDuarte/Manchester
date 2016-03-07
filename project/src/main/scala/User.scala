import breeze.stats.distributions.Rand

abstract class User(val id: String) {
  def emitAction(currentPage: Page, website: Website): Action

  def canEqual(other: Any): Boolean = other.isInstanceOf[User]

  override def equals(other: Any): Boolean = other match {
    case that: User =>
      (that canEqual this) &&
        id == that.id
    case _ => false
  }

  override def hashCode(): Int = {
    id.hashCode
  }
}

case class RandomUser(userId: String) extends User(userId) {
  override def emitAction(currentPage: Page, website: Website): Action = {
    if (Rand.randInt(3).draw() == 2 /* 33.3% */ || currentPage.links.isEmpty)
      ExitAction()
    else if (Rand.randInt(101).draw() <= 5 /* 5% */ && currentPage.tags.contains(website.pageTypes.product)) {
      val cartPage = currentPage.links.find(l => l.tags.contains(website.pageTypes.cart)).get
      AddToCartAction(currentPage, cartPage)
    } else {
      val nextPage = Rand.choose(currentPage.links).draw()
      BrowseToAction(nextPage)
    }
  }
}

case class AffinityUser(userId: String, affinities: Map[String, Double] = Map()) extends User(userId) {
  override def emitAction(currentPage: Page, website: Website): Action = {
    if (Rand.randInt(3).draw() == 2 /* 33.3% */ || currentPage.links.isEmpty)
      ExitAction()
    else if (Rand.randInt(101).draw() <= 5 /* 5% */ && currentPage.tags.contains(website.pageTypes.product)) {
      // assumed that a product page links to a cart page
      val cartPage = currentPage.links.find(l => l.tags.contains(website.pageTypes.cart)).get
      AddToCartAction(currentPage, cartPage)
    } else {
      val affSelected = RandHelper.choose(affinities).draw()

      val links = currentPage.links.filter(p => p.tags.contains(affSelected) &&
        (p.tags.contains(website.pageTypes.product) || p.tags.contains(website.pageTypes.list)))

      val nextPage = if (links.isEmpty)
        Rand.choose(currentPage.links).draw()
      else
        Rand.choose(links).draw()

      BrowseToAction(nextPage)
    }
  }
}