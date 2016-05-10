import breeze.stats.distributions.Rand

case class RandomFactory() extends UserFactory[RandomUser] {

  def getNewUserId: String = java.util.UUID.randomUUID().toString

  val users: Iterator[List[RandomUser]] = {
    Iterator.continually(List.fill(25)(RandomUser(getNewUserId)))
  }
}

case class RandomUser(id: String) extends User {
  override def emitAction(currentPage: Page, website: Website): Action = {
    if (Rand.randInt(3).draw() == 2 /* 33.3% */ || currentPage.links.isEmpty)
      ExitAction()
    else if (Rand.randInt(101).draw() <= 5 /* 5% */ && currentPage.tags.contains(PageTypesTags.product)) {
      val cartPage = currentPage.links.find(l => l.tags.contains(PageTypesTags.cart)).get
      AddToCartAction(currentPage.product.get, cartPage)
    } else {
      val nextPage = Rand.choose(currentPage.links).draw()
      BrowseToAction(nextPage)
    }
  }
}
