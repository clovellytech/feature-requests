package h4sm.featurerequests

import cats.effect._
import cats.syntax.all._
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import h4sm.auth.infrastructure.endpoint.{AuthEndpoints, Authenticators}
import h4sm.auth.infrastructure.repository.persistent.{
  TokenRepositoryInterpreter,
  UserRepositoryInterpreter,
}
import h4sm.auth.domain.tokens._
import h4sm.auth.domain.UserService
import h4sm.db.config._
import h4sm.featurerequests.config._
import io.circe.config.parser
import infrastructure.endpoint._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._

import scala.concurrent.ExecutionContext
import tsec.passwordhashers.jca.BCrypt
import tsec.authentication.TSecBearerToken

class Server[F[_]: ConcurrentEffect: Timer: ContextShift](ec: ExecutionContext) {
  def app(xa: Transactor[F], port: Int, host: String): F[ExitCode] = {
    implicit val userAlg = new UserRepositoryInterpreter(xa)
    val userService = new UserService[F, BCrypt](BCrypt)
    implicit val tokenService = new TokenRepositoryInterpreter(xa)
    val authEndpoints = new AuthEndpoints(userService, Authenticators.bearer)
    val authService = authEndpoints.Auth
    val requestEndpoints = RequestEndpoints.persistingEndpoints[F, TSecBearerToken](xa)
    val voteEndpoints = VoteEndpoints.persistingEndpoints[F, TSecBearerToken](xa)
    val requestApp = requestEndpoints.unAuthEndpoints <+> authService.liftService(
      requestEndpoints.authEndpoints,
    )
    val httpApp = Router(
      "/auth" -> authEndpoints.endpoints,
      "/" -> requestApp,
      "/vote" -> authService.liftService(voteEndpoints.endpoints),
    ).orNotFound
    BlazeServerBuilder[F](ec)
      .bindHttp(port, host)
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }

  def run(connEc: ExecutionContext, blk: Blocker): F[ExitCode] =
    for {
      cfg <- parser.decodeF[F, FeatureRequestConfig]()
      FeatureRequestConfig(host, port, db) = cfg
      _ <- ConcurrentEffect[F].delay(
        DatabaseConfig.initialize(db)("ct_auth", "ct_feature_requests"),
      )
      exitCode <-
        HikariTransactor
          .newHikariTransactor(db.driver, db.url, db.user, db.password, connEc, blk)
          .use(app(_, port, host))
    } yield exitCode
}
