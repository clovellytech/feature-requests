package h4sm.petstore
package infrastructure.repository.persistent

import cats.data.OptionT
import cats.effect.Bracket
import cats.syntax.all._
import db.sql._
import domain._
import doobie._
import doobie.implicits._

// I require a Bracket[?[_], Throwable] because doobie's transact requires it.
class OrderRepository[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F]) extends OrderAlgebra[F] {
  // Members declared in h4sm.db.CAlgebra
  def insert(a: Order): F[Unit] = orders.insert(a).run.void.transact(xa)

  def insertGetId(a: Order): OptionT[F, OrderId] = OptionT.liftF(orders.insertGetId(a).transact(xa))

  // Members declared in h4sm.db.RAlgebra
  def byId(id: OrderId): OptionT[F, Annotated] = OptionT(orders.selectById(id).option.transact(xa))
  def select: F[List[Annotated]] = orders.select.to[List].transact(xa)

  def setShipped(orderId: OrderId): F[Unit] = orders.setShipped(orderId).run.void.transact(xa)

  def delete(i: OrderId): F[Unit] = orders.delete(i).run.transact(xa).void
}
