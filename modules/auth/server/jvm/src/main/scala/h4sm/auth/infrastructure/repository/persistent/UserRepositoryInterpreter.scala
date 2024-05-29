package h4sm.auth
package infrastructure.repository.persistent

import cats.data.OptionT
import cats.syntax.all._
import cats.effect.Bracket
import doobie._
import doobie.implicits._
import db.domain.User
import domain.users.UserRepositoryAlgebra
import db.sql._

class UserRepositoryInterpreter[M[_]: Bracket[?[_], Throwable]](xa: Transactor[M])
    extends UserRepositoryAlgebra[M] {
  def insert(a: User): M[Unit] =
    users
      .insert(a)
      .run
      .as(())
      .exceptSomeSqlState { case UNIQUE_VIOLATION =>
        HC.rollback
      }
      .transact(xa)

  def insertGetId(a: User): OptionT[M, UserId] =
    OptionT {
      val q: ConnectionIO[Option[UserId]] = users.insertGetId(a).map(_.some)
      q.exceptSomeSqlState { case UNIQUE_VIOLATION =>
        HC.rollback.as(none[UserId])
      }.transact(xa)
    }

  def select: M[List[(User, UserId, Instant)]] = users.select.to[List].transact(xa)

  def byId(id: UserId): OptionT[M, (User, UserId, Instant)] =
    OptionT(users.selectById(id).option.transact(xa))

  def byUsername(username: String): OptionT[M, (User, UserId, Instant)] =
    OptionT(users.byUsername(username).option.transact(xa))

  def delete(i: UserId): M[Unit] = users.delete(i).run.as(()).transact(xa)

  def update(id: UserId, u: User): M[Unit] = users.update(id, u).run.as(()).transact(xa)

  def updateUnique(u: User): M[Unit] =
    (for {
      uq <- byUsername(u.username)
      (user, id, _) = uq
      res <- OptionT(update(id, user).map(_.some))
    } yield res).value.map(_.getOrElse(()))
}
