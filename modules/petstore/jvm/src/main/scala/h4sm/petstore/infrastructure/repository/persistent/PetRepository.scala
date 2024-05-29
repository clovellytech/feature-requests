package h4sm.petstore
package infrastructure.repository.persistent

import cats.effect.Bracket
import cats.syntax.all._
import cats.data.OptionT
import db.sql._
import domain._
import doobie._
import doobie.implicits._

// I require a Bracket[?[_], Throwable] because doobie's transact requires it.
class PetRepository[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F]) extends PetAlgebra[F] {
  // Members declared in h4sm.db.CAlgebra
  def insert(a: Pet): F[Unit] = pets.insert(a).run.transact(xa).void

  def insertGetId(a: Pet): OptionT[F, PetId] = OptionT.liftF(pets.insertGetId(a).transact(xa))

  // Members declared in h4sm.db.RAlgebra
  def byId(id: PetId): OptionT[F, Annotated] = OptionT(pets.selectById(id).option.transact(xa))
  def select: F[List[Annotated]] = pets.select.to[List].transact(xa)

  // Members declared in h4sm.db.UAlgebra
  def update(id: PetId, u: Pet): F[Unit] = pets.update(id, u).run.void.transact(xa)

  def delete(i: PetId): F[Unit] = pets.delete(i).run.transact(xa).void
}
