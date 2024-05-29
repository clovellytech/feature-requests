package h4sm
package invitations
package infrastructure
package repository.persistent

import auth.UserId
import cats.data.OptionT
import cats.effect.Bracket
import cats.syntax.all._
import domain._
import doobie._
import doobie.implicits._
import java.time.Instant
import sql.invitation

class InvitationRepository[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F])
    extends InvitationAlgebra[F] {
  // Members declared in h4sm.db.CAlgebra
  def insert(a: Invitation[UserId]): F[Unit] = invitation.insert(a).run.transact(xa).void
  def insertGetId(a: Invitation[UserId]): OptionT[F, InvitationId] =
    OptionT.liftF(invitation.insertGetId(a).transact(xa))

  // Members declared in h4sm.db.RAlgebra
  def byId(id: InvitationId): OptionT[F, (Invitation[UserId], InvitationId, Instant)] =
    OptionT(invitation.byId(id).option.transact(xa))

  def select: F[List[(Invitation[UserId], InvitationId, Instant)]] =
    invitation.all.to[List].transact(xa)

  // Members declared in h4sm.db.DAlgebra
  def delete(i: InvitationId): F[Unit] = invitation.delete(i).run.transact(xa).void

  def fromCode(toEmail: String, code: String): OptionT[F, Annotated] =
    OptionT(invitation.byCode(toEmail, code).option.transact(xa))

  def opened(invitationId: InvitationId): F[Unit] =
    invitation.updateOpenTime(invitationId).run.transact(xa).void

  def fromToEmail(toEmail: String): OptionT[F, Annotated] =
    OptionT(invitation.fromToEmail(toEmail).option.transact(xa))
}
