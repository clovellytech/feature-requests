package h4sm
package invitations.client

import auth.UserId
import auth.client.AuthClientRunner
import auth.comm.UserRequest
import auth.domain.tokens._
import cats.effect.{Bracket, Sync}
import cats.syntax.all._
import invitations.domain._
import invitations.infrastructure.repository.persistent.InvitationRepository
import invitations.infrastructure.endpoint._
import org.http4s.Headers
import tsec.authentication.TSecBearerToken
import tsec.passwordhashers.PasswordHasher
import tsec.passwordhashers.jca.BCrypt
import java.time.Instant

abstract class InvitationClientRunner[F[_]: Sync: Bracket[?[_], Throwable]]
    extends AuthClientRunner[F] {
  implicit lazy val invitationAlg: InvitationAlgebra[F] = new InvitationRepository[F](xa)
  def invitationEndpoints(implicit P: PasswordHasher[F, BCrypt]) =
    new InvitationEndpoints[F, BCrypt, TSecBearerToken](userService, auth)
  def invitationClient(implicit P: PasswordHasher[F, BCrypt]) =
    new InvitationsClient[F, BCrypt, TSecBearerToken](invitationEndpoints)

  def createInvite(toName: String, toUser: UserRequest)(implicit
      h: Headers,
      P: PasswordHasher[F, BCrypt],
  ): F[(Invitation[UserId], InvitationId, Instant)] =
    for {
      _ <- invitationClient.sendInvite(InvitationRequest(toName, toUser.username))
      res <-
        invitationAlg
          .fromToEmail(toUser.username)
          .getOrElseF(Sync[F].raiseError(new Exception("Invitation not found")))
    } yield res
}
