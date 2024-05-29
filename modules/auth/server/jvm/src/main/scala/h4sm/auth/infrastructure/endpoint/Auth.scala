package h4sm.auth
package infrastructure.endpoint

import scala.concurrent.duration._
import cats.data.OptionT
import cats.effect._
import cats.syntax.all._
import comm.{SiteResult, UserDetail, UserRequest}
import comm.codecs._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec._
import org.http4s.headers.`WWW-Authenticate`
import tsec.authentication._
import domain._
import domain.users.UserRepositoryAlgebra
import domain.tokens.{AsBaseToken, TokenRepositoryAlgebra}
import infrastructure.authentication.TransBackingStore._
import domain.tokens.AsBaseToken.ops._
import domain.tokens._
import tsec.cipher.symmetric.{AES, IvGen}
import tsec.cipher.symmetric.jca._
import h4sm.auth.comm.UserDetailId

object Authenticators {
  def statelessCookie[F[_]: Sync: UserRepositoryAlgebra, Alg: JAuthEncryptor[F, ?]: IvGen[F, ?]](
      key: SecretKey[Alg],
      expiryDuration: FiniteDuration = 10.minutes,
      maxIdle: Option[FiniteDuration] = None,
      secure: Boolean = false,
      domain: Option[String] = None,
  )(implicit a: AES[Alg]): UserAuthenticator[F, AuthEncryptedCookie[Alg, ?]] =
    EncryptedCookieAuthenticator.stateless(
      TSecCookieSettings(
        cookieName = "ct-auth",
        secure,
        domain = domain,
        expiryDuration = expiryDuration,
        maxIdle = maxIdle,
      ),
      userTrans(UserRepositoryAlgebra[F]),
      key,
    )

  def bearer[F[_]: Sync: UserRepositoryAlgebra: TokenRepositoryAlgebra]
      : UserAuthenticator[F, TSecBearerToken] =
    BearerTokenAuthenticator(
      tokenTrans[TSecBearerToken].apply(TokenRepositoryAlgebra[F]),
      userTrans(UserRepositoryAlgebra[F]),
      TSecTokenSettings(
        expiryDuration = 10.minutes,
        maxIdle = None,
      ),
    )
}

class AuthEndpoints[F[_]: Sync: UserRepositoryAlgebra, A, T[_]](
    userService: UserService[F, A],
    authenticator: UserAuthenticator[F, T],
)(implicit A: AsBaseToken[T[UserId]])
    extends Http4sDsl[F] {
  type Token = T[UserId]

  val Auth = SecuredRequestHandler(authenticator)

  val badResp = Unauthorized(
    `WWW-Authenticate`(
      Challenge(
        "Digest",
        "ct_auth",
        Map("username" -> "bad username", "password" -> "bad password"),
      ),
    ),
  )

  val unauthService: HttpRoutes[F] = HttpRoutes.of {
    case req @ POST -> Root / "user" =>
      val res: F[Response[F]] = for {
        userRequest <- req.as[UserRequest]
        _ <- userService.signupUser(userRequest.username, userRequest.password)
        result <- Ok()
      } yield result

      res.recoverWith {
        case _: Error.Duplicate => Conflict("Username already exists")
        case _ => BadRequest()
      }

    case req @ POST -> Root / "login" =>
      val res: F[Response[F]] = for {
        userRequest <- req.as[UserRequest]
        (user, userId) <- userService.lookup(userRequest.username, userRequest.password)
        resp <- Ok(SiteResult(userRequest.username))
        tok <- authenticator.create(userId)
      } yield authenticator.embed(resp, tok)

      res.recoverWith {
        case _: Error.BadLogin => badResp
        case _: Error.NotFound => badResp
        case _ => BadRequest("Not found")
      }

    case GET -> Root / "exists" / username =>
      UserRepositoryAlgebra[F].byUsername(username).isDefined.flatMap(Ok.apply(_))
  }

  val authService: UserAuthService[F, T] = UserAuthService {

    case req @ GET -> Root asAuthed _ =>
      for {
        (user, userId, joinTime) <- userService.byUserId(req.authenticator.asBase.identity)
        resp <- Ok(SiteResult(UserDetailId(user.username, joinTime, userId)))
      } yield resp

    case req @ GET -> Root / "user" asAuthed _ =>
      for {
        (user, _, joinTime) <- userService.byUserId(req.authenticator.asBase.identity)
        resp <- Ok(SiteResult(UserDetail(user.username, joinTime)))
      } yield resp

    case GET -> Root / "user" / name asAuthed _ =>
      for {
        (user, _, joinTime) <- userService.byUsername(name)
        resp <- Ok(SiteResult(UserDetail(user.username, joinTime)))
      } yield resp

    case req @ POST -> Root / "logout" asAuthed _ =>
      authenticator.discard(req.authenticator) *> Ok()
  }

  def testService: HttpRoutes[F] =
    HttpRoutes.of {
      case GET -> Root / "istest" => Ok(true)
      case DELETE -> Root / username =>
        (for {
          u <- UserRepositoryAlgebra[F].byUsername(username)
          _ <- OptionT.liftF(UserRepositoryAlgebra[F].delete(u._2))
          resp <- OptionT.liftF(Ok())
        } yield resp).getOrElseF(BadRequest())
    }

  def endpoints = unauthService <+> Auth.liftService(authService)
}
