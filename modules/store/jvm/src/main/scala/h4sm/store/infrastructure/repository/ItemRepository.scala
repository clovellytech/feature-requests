package h4sm.store
package infrastructure.repository

import cats.data.OptionT
import cats.effect.Bracket
import cats.syntax.all._
import doobie.Transactor
import doobie.implicits._
import domain._

class ItemRepository[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F]) extends ItemAlgebra[F] {
  // Members declared in h4sm.db.CAlgebra
  def insert(a: Item): F[Unit] = sql.item.insert(a).run.void.transact(xa)
  def insertGetId(a: Item): OptionT[F, ItemId] = OptionT.liftF(sql.item.insertGetId(a).transact(xa))

  // Members declared in h4sm.db.DAlgebra
  def delete(i: ItemId): F[Unit] = sql.item.delete(i).run.void.transact(xa)

  // Members declared in h4sm.db.RAlgebra
  def byId(id: ItemId): OptionT[F, (Item, ItemId, Instant)] =
    OptionT(sql.item.byId(id).option.transact(xa))
  def select: F[List[(Item, ItemId, Instant)]] = sql.item.select.to[List].transact(xa)

  // Members declared in h4sm.db.UAlgebra
  def update(id: ItemId, u: Item): F[Unit] = sql.item.update(id, u).run.void.transact(xa)

  def byIds(ids: List[ItemId]): F[List[Annotated]] = sql.item.byIds(ids).to[List].transact(xa)
}
