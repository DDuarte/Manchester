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