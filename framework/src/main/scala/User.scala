import breeze.stats.distributions.{DiscreteDistr, Rand}

import scala.concurrent.duration.Duration

trait UserFactory[+T] {
  def users: Iterator[List[T]]
  def getNewUserId: String
}

trait User {
  def emitAction(currentPage: Page, website: Website): Action
  def id: String
}
