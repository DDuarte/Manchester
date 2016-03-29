abstract class Action
case class BrowseToAction(page: Page) extends Action
case class ExitAction() extends Action
case class AddToCartAction(product: Page /* FIXME */, cartPage: Page) extends Action
case class IdleAction(tickCount: Long) extends Action
