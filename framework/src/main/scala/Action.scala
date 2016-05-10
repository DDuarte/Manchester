abstract class Action
case class BrowseToAction(page: Page) extends Action
case class ExitAction() extends Action
case class AddToCartAction(product: Product, cartPage: Page) extends Action
case class IdleAction(tickCount: Long) extends Action // TODO implement
case class PurchaseAction() extends Action // TODO implement
