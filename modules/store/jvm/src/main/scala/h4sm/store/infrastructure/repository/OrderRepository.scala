package h4sm.store
package infrastructure.repository

import cats.effect.Bracket
import cats.syntax.all._
import cats.data.OptionT
import doobie._
import doobie.implicits._
import domain._

class OrderRepository[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F]) extends OrderAlgebra[F] {
  // Members declared in h4sm.db.CAlgebra
  def insert(a: Order): F[Unit] = insertIdF(a).void

  def insertGetId(a: Order): OptionT[F, OrderId] = OptionT.liftF(insertIdF(a))

  def insertIdF(a: Order): F[OrderId] =
    (for {
      oid <- sql.order.insertGetId(a)
      _ <- a.items.traverse(sql.order.insertOrderItem(oid, _).run)
    } yield oid).transact(xa)

  // Members declared in h4sm.db.RAlgebra
  def byId(id: OrderId): OptionT[F, (Order, OrderId, Instant)] =
    OptionT(sql.order.byId(id).option.transact(xa))

  def select: F[List[(Order, OrderId, Instant)]] = sql.order.select.to[List].transact(xa)

  // Members declared in h4sm.db.DAlgebra
  def delete(id: OrderId): F[Unit] = sql.order.delete(id).run.void.transact(xa)

  // Members declared in h4sm.db.UAlgebra
  def update(id: OrderId, u: Order): F[Unit] = sql.order.update(id, u).run.void.transact(xa)

  // Members declared in h4sm.store.domain.OrderAlgebra
  def insertOrderItem(orderId: OrderId, orderItem: OrderItem): F[Unit] =
    sql.order.insertOrderItem(orderId, orderItem).run.void.transact(xa)

  def submitOrder(orderId: OrderId): F[Unit] =
    sql.order.setSubmitted(orderId).run.void.transact(xa)
}
