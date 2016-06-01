trait WebsiteAgent {
  def id: String
  def modifyPage(page: Page, visitor: User): Page
  def notifyUserAction(user: User, currentPage: Option[Page], action: Action)
}

case class DummyWebsiteAgent() extends WebsiteAgent {
  val id = "dummy"
  def modifyPage(page: Page, visitor: User): Page = page
  def notifyUserAction(user: User, currentPage: Option[Page], action: Action) {}
}

case class CollaborativeFilteringRecommendationWebsiteAgent() extends WebsiteAgent {
  def id = "collab_filter"
  def modifyPage(page: Page, visitor: User): Page = ???
  def notifyUserAction(user: User, currentPage: Option[Page], action: Action): Unit = ???
}

case class MostFrequentRecommendationWebsiteAgent() extends WebsiteAgent {
  def id = "most_freq"
  def modifyPage(page: Page, visitor: User): Page = ???
  def notifyUserAction(user: User, currentPage: Option[Page], action: Action): Unit = ???
}
