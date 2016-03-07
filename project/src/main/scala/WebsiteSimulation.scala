import breeze.stats.distributions.Poisson

class WebsiteSimulation(website: Website) extends Simulation {
  // System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer")

  val state = new WebsiteStateVisualization(website)

  def newUsers() {

    val personas = Map(
      Map(
        "Portáteis" -> 1.0,
        "HP" -> 1.0
      ) -> 1.0
    )

    val distribution = Poisson(25)

    val newUsers = distribution.draw()
    println(s"New users: $newUsers")

    for (i <- 0 until newUsers) {
      // val user = new RandomUser(lastUserId.toString)
      val user = AffinityUser(state.newUserId.toString, RandHelper.choose(personas).draw())
      state.users.put(user, website.homepage)
      state.visitPage(website.homepage)
      state.newUser()
    }
  }

  def userInjector() {
    if (currentTime < 100) {
      schedule(1) {
        newUsers()
        state.users.foreach { case (user: User, page: Page) =>
          val action = user.emitAction(page, website)
          action match {
            case browse: BrowseToAction =>
              val prevPage = page
              val nextPage = browse.page
              state.users.update(user, nextPage)
              state.visitPage(nextPage)
              println(s"User ${user.id} went from page ${prevPage.id} to ${nextPage.id}")
            case addToCart: AddToCartAction =>
              state.addToCart(addToCart.product)
              state.visitPage(addToCart.cartPage)
              state.visitPage(website.homepage)
              state.users.update(user, website.homepage)
              println(s"User ${user.id} added ${addToCart.product.id} to cart, back to homepage")
            case exit: ExitAction =>
              state.users.remove(user)
              println(s"User ${user.id} exited")
          }

          sleep(0) // animation purposes
        }

        userInjector()
      }
    }
  }

  schedule(0) {
    userInjector()
  }

  def sleep(ms: Int = 50) {
    try {
      Thread.sleep(ms)
    } catch {
      case _: Throwable =>
    }
  }
}
