package h4sm.messages.infrastructure.repository.persistent

import cats.data.OptionT
import cats.effect.Bracket
import cats.syntax.all._
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import fs2.Stream
import h4sm.auth.UserId
import h4sm.messages.domain._
import h4sm.messages.infrastructure.repository.persistent.sql.messages

class MessageRepository[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F])
    extends MessageAlgebra[F] {

  // Members declared in h4sm.db.CAlgebra
  def insert(a: UserMessage): F[Unit] =
    messages.insert(a).run.as(()).transact(xa)

  def insertGetId(a: UserMessage): OptionT[F, MessageId] =
    OptionT.liftF(messages.insert(a).withUniqueGeneratedKeys[MessageId]("message_id")).transact(xa)

  // Members declared in h4sm.db.DAlgebra
  def delete(i: MessageId): F[Unit] =
    messages.delete(i).run.as(()).transact(xa)

  // Members declared in h4sm.db.RAlgebra
  def byId(id: MessageId): cats.data.OptionT[F, Annotated] =
    OptionT(messages.byId(id).option).transact(xa)
  def select: F[List[Annotated]] = messages.select.to[List].transact(xa)

  def inbox(userId: UserId): Stream[F, Annotated] =
    messages.inbox(userId).stream.transact(xa)

  def thread(from: UserId, to: UserId): Stream[F, Annotated] =
    messages.thread(from, to).stream.transact(xa)
}
