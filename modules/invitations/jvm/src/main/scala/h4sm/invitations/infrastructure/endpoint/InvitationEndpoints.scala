package h4sm
package invitations
package infrastructure.endpoint

import auth.{domain => _, _}
import auth.domain._
import auth.domain.tokens.AsBaseToken
import auth.domain.tokens.AsBaseToken.ops._
import cats.effect.Sync
import cats.syntax.all._
import domain._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec._
import tsec.authentication._
import h4sm.invitations.infrastructure.endpoint.codecs._

class InvitationEndpoints[F[_]: Sync: InvitationAlgebra, A, T[_]](
    userService: UserService[F, A],
    authenticator: UserAuthenticator[F, T],
)(implicit A: AsBaseToken[T[UserId]])
    extends Http4sDsl[F] {
  val srh = SecuredRequestHandler(authenticator)

  def inviteFromCode(
      email: String,
      inviteCode: String,
  ): F[(Invitation[UserId], InvitationId, Instant)] =
    InvitationAlgebra[F]
      .fromCode(email, inviteCode)
      .toRight[Throwable](InvitationError.invalid)
      .rethrowT

  def addInvite: UserAuthService[F, T] =
    UserAuthService { case req @ POST -> Root asAuthed _ =>
      for {
        inv <- req.request.as[InvitationRequest]
        _ <- InvitationAlgebra[F].insert(
          Invitation[UserId](req.authenticator.asBase.identity, inv.toName, inv.toEmail),
        )
        res <- Ok()
      } yield res
    }

  def openInvite: HttpRoutes[F] =
    HttpRoutes.of { case _ @GET -> Root / "open" / inviteCode / emailStr =>
      for {
        invitation <- inviteFromCode(emailStr, inviteCode)
        _ <- InvitationAlgebra[F].opened(invitation._2)
        res <- Ok()
      } yield res
    }

  def signupInvite: HttpRoutes[F] =
    HttpRoutes.of { case req @ POST -> Root / "signup" =>
      val res: F[Response[F]] = for {
        signupReq <- req.as[InviteSignup]
        _ <- inviteFromCode(signupReq.email, signupReq.inviteCode)
        _ <- userService.signupUser(signupReq.email, signupReq.password)
        res <- Ok()
      } yield res

      res.recoverWith {
        case _: Error.Duplicate => Conflict("Email address already registered")
        case _ => BadRequest("Unknown error")
      }
    }

  val authService = addInvite

  val authEndpoints = srh.liftService(authService)
  val unauthEndpoints = openInvite

  val endpoints = openInvite <+> authEndpoints
}
