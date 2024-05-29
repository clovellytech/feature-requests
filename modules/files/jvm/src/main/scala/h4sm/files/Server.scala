package h4sm.files

import cats.effect._
import cats.syntax.all._
import h4sm.auth.domain.tokens._
import h4sm.auth.domain._
import h4sm.auth.infrastructure.endpoint.{AuthEndpoints, Authenticators}
import h4sm.db.config.DatabaseConfig
import h4sm.files.config._
import h4sm.files.infrastructure.endpoint.FileEndpoints
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import h4sm.auth.infrastructure.repository.persistent.{
  TokenRepositoryInterpreter,
  UserRepositoryInterpreter,
}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import tsec.passwordhashers.jca.BCrypt
import infrastructure.backends._
import org.http4s.server.Router

import scala.concurrent.ExecutionContext

class Server[F[_]: ConcurrentEffect: ConfigAsk: ContextShift: Timer: ServerConfigAsk: DBConfigAsk](
    ec: ExecutionContext,
) {
  def app(xa: Transactor[F], serverConf: ServerConfig, blk: Blocker): F[ExitCode] = {
    implicit val b = blk
    implicit val userAlg = new UserRepositoryInterpreter(xa)
    val userService = new UserService[F, BCrypt](BCrypt)
    implicit val tokenService = new TokenRepositoryInterpreter(xa)
    val authEndpoints = new AuthEndpoints(userService, Authenticators.bearer)
    implicit val fileMetaStorage = new FileMetaService(xa)(ConcurrentEffect[F])
    implicit val fileStorage = new LocalFileStoreService[F]
    val fileEndpoints = new FileEndpoints(authEndpoints.Auth)

    val httpApp = Router(
      "/auth" -> authEndpoints.endpoints,
      "/files" -> fileEndpoints.endpoints,
    ).orNotFound

    BlazeServerBuilder[F](ec)
      .bindHttp(serverConf.port, serverConf.host)
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }

  def run(implicit connEc: ExecutionContext, blk: Blocker): F[ExitCode] =
    for {
      db <- DBConfigAsk[F].ask
      serverConf <- ServerConfigAsk[F].ask
      _ <- ConfigAsk[F].ask
      _ <- ConcurrentEffect[F].delay(DatabaseConfig.initialize(db)("ct_auth", "ct_files"))
      exitCode <-
        HikariTransactor
          .newHikariTransactor(db.driver, db.url, db.user, db.password, connEc, blk)
          .use(app(_, serverConf, blk))
    } yield exitCode
}
