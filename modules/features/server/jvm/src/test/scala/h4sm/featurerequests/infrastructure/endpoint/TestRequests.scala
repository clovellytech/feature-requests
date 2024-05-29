package h4sm
package featurerequests
package infrastructure.endpoint

import auth.client.AuthClient
import auth.domain.UserService
import auth.infrastructure.endpoint.Authenticators
import auth.infrastructure.repository.persistent._
import cats.effect.Sync
import cats.syntax.all._
import doobie.util.transactor.Transactor
import featurerequests.comm.codecs._
import featurerequests.comm.domain.features._
import featurerequests.comm.domain.votes._
import infrastructure.repository.persistent.{
  RequestRepositoryInterpreter,
  VoteRepositoryInterpreter,
}
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl._
import org.http4s.client.dsl._
import org.http4s.circe.CirceEntityCodec._
import tsec.passwordhashers.jca.BCrypt
import tsec.authentication.TSecBearerToken

class TestRequests[F[_]: Sync](xa: Transactor[F]) extends Http4sDsl[F] with Http4sClientDsl[F] {
  implicit val userRepo = new UserRepositoryInterpreter(xa)
  val userService = new UserService[F, BCrypt](BCrypt)
  implicit val tokenService = new TokenRepositoryInterpreter(xa)
  val authenticator = Authenticators.bearer[F]
  val authTestEndpoints = new AuthClient(userService, authenticator)

  val authEndpoints = authTestEndpoints.authEndpoints
  val Auth = authEndpoints.Auth

  implicit val rinterp = new RequestRepositoryInterpreter(xa)
  val re = new RequestEndpoints[F, TSecBearerToken]

  val requestEndpoints: HttpRoutes[F] = re.unAuthEndpoints <+> Auth.liftService(re.authEndpoints)

  implicit val vinterp = new VoteRepositoryInterpreter[F](xa)
  val voteEndpoints: HttpRoutes[F] =
    Auth.liftService(new VoteEndpoints[F, TSecBearerToken].endpoints)

  def addRequest(req: FeatureRequest)(resp: Response[F]): F[Response[F]] =
    for {
      addReq <- POST(req, Uri.uri("/request"))
      authReq = authTestEndpoints.injectAuthHeader(resp)(addReq)
      resp <- requestEndpoints.orNotFound.run(authReq)
    } yield resp

  def addVote(vote: VoteRequest): F[Response[F]] =
    for {
      voteReq <- POST(vote, Uri.uri("/vote"))
      resp <- voteEndpoints.orNotFound.run(voteReq)
    } yield resp

  def getRequests: F[Response[F]] =
    GET(Uri.uri("/request")).flatMap(requestEndpoints.orNotFound.run(_))
}
