class WebsiteSimulation(
    val website:      Website,
    val userFactory:  UserFactory[User],
    val websiteAgent: WebsiteAgent,
    val websiteState: Option[WebsiteState] = None
) extends Simulation {

  lazy val state = websiteState match {
    case Some(ws) => ws
    case None => new WebsiteState
  }

  override def run(): Unit = {
    def newUsers() {
      val newUsers = userFactory.users.next()
      println(s"New users: ${newUsers.length}")

      for (user <- newUsers) {
        state.newUser()
        websiteAgent.notifyUserAction(user, None, BrowseToAction(website.homepage))
        state.visitPage(user, websiteAgent.modifyPage(website.homepage, user))
      }
    }

    def userInjector() {
      if (currentTime < 10) {
        schedule(1) {
          newUsers()
          state.users.foreach {
            case (user: User, page: Page) =>
              val action = user.emitAction(page, website)
              websiteAgent.notifyUserAction(user, Some(page), action)
              action match {
                case browse: BrowseToAction =>
                  val prevPage = page
                  val nextPage = browse.page
                  state.visitPage(user, websiteAgent.modifyPage(nextPage, user))
                //println(s"User ${user.id} went from page ${prevPage.id} to ${nextPage.id}")
                case addToCart: AddToCartAction =>
                  state.addToCart(addToCart.product)
                  state.visitPage(user, websiteAgent.modifyPage(addToCart.cartPage, user))
                  state.visitPage(user, websiteAgent.modifyPage(website.homepage, user))
                //println(s"User ${user.id} added ${addToCart.product.id} to cart, back to homepage")
                case exit: ExitAction =>
                  state.exit(user)
                //println(s"User ${user.id} exited")
              }

              sleep(0) // animation purposes
          }

          userInjector()
        }
      }
    }

    state.startSimulation()

    schedule(0) {
      userInjector()
    }

    while (hasNext)
      step()

    state.finishSimulation()

    def sleep(ms: Int = 50) {
      try {
        Thread.sleep(ms)
      } catch {
        case _: Throwable =>
      }
    }
  }
}
