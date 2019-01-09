package h4sm

import cats.effect._
import cats.implicits._
import doobie._
import auth.infrastructure.endpoint.AuthEndpoints
import doobie.hikari.HikariTransactor
import h4sm.db.config._
import h4sm.files.infrastructure.endpoint.FileEndpoints
import org.http4s.HttpRoutes
import org.http4s.server.{Router, Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import tsec.passwordhashers.jca.BCrypt

import scala.concurrent.ExecutionContext


/*
 * Build a server that uses every module in this project...
 */
class H4SMServer[F[_] : ContextShift : ConcurrentEffect : Timer : files.config.ConfigAsk](implicit
  C : ConfigAsk[F]
) {

  def router(xa : Transactor[F], testMode : Boolean) : HttpRoutes[F] = {
    val auth = AuthEndpoints.persistingEndpoints[F, BCrypt](xa)
    val files = FileEndpoints.persistingEndpoints[F](xa, auth, ExecutionContext.Implicits.global)

    Router(
      "/users" -> {
        if (testMode) auth.testService <+> auth.endpoints
        else auth.endpoints
      },
      "/files" -> files.endpoints
    )
  }

  def createServer : Resource[F, Server[F]] = for {
    cfg <- Resource.liftF(C.ask)
    MainConfig(db, fc, ServerConfig(host, port, numThreads), test) = cfg
    _ = if (test) println("DANGER, RUNNING IN TEST MODE!!")
    connec <- ExecutionContexts.fixedThreadPool[F](numThreads)
    tranec <- ExecutionContexts.cachedThreadPool[F]
    xa <- HikariTransactor.newHikariTransactor[F](db.driver, db.url, db.user, db.password, connec, tranec)
    _ <- Resource.liftF(DatabaseConfig.initialize[F](db)("ct_auth", "ct_files"))

    server <- BlazeServerBuilder[F]
              .bindHttp(port, host)
              .withHttpApp(router(xa, cfg.test).orNotFound)
              .resource
  } yield server
}

object ServerMain extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    implicit val cfg : ConfigAsk[IO] = getConfigAsk[IO].map { (c : MainConfig) =>
      args match {
        case "test" :: _ => c.copy(test = true)
        case _ => c
      }
    }

    implicit val fcfg : files.config.ConfigAsk[IO] = getFileConfigAsk

    val server = new H4SMServer[IO]

    server.createServer.use(_ => IO.never).as(ExitCode.Success)
  }
}
