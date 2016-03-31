

class WebsiteSimulation(website: Website, profiles: Map[UserProfile, Double]) extends Simulation {
  // System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer")

  val state = new WebsiteStateVisualization(website)

  def newUsers() {

    val profile = RandHelper.choose(profiles).draw()

    val distribution = profile.arrivalDistribution

    val newUsers = distribution.draw()
    println(s"New users: $newUsers")

    for (i <- 0 until newUsers) {
      val user = AffinityUser(state.newUserId.toString, profile)
      state.visitPage(user, website.homepage)
      state.newUser()
    }
  }

  def userInjector() {
    if (currentTime < 10) {
      schedule(1) {
        newUsers()
        state.users.foreach {
          case (user: User, page: Page) =>
            val action = user.emitAction(page, website)
            action match {
              case browse: BrowseToAction =>
                val prevPage = page
                val nextPage = browse.page
                state.visitPage(user, nextPage)
              //println(s"User ${user.id} went from page ${prevPage.id} to ${nextPage.id}")
              case addToCart: AddToCartAction =>
                state.addToCart(addToCart.product)
                state.visitPage(user, addToCart.cartPage)
                state.visitPage(user, website.homepage)
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
