package h4sm
package permissions
package client

import cats.data.Kleisli
import cats.syntax.all._
import cats.effect.Sync
import domain._
import infrastructure.endpoint._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.circe.CirceEntityCodec._
import testutil.infrastructure.endpoints._
import h4sm.auth.comm.SiteResult
import h4sm.auth.comm.codecs._
import h4sm.permissions.infrastructure.endpoint.codecs._

class PermissionClient[F[_]: Sync, Alg, T[_]](es: PermissionEndpoints[F, T])
    extends Http4sDsl[F]
    with Http4sClientDsl[F]
    with SessionClientDsl[F] {
  val endpoints: Kleisli[F, Request[F], Response[F]] = es.endpoints.orNotFound

  def addPermission(p: Permission)(implicit h: Headers): F[Unit] =
    for {
      req <- post(p, Uri.uri("/"))
      resp <- endpoints.run(req)
      _ <- passOk(resp)
    } yield ()

  def getPermissions(appName: String): F[List[(Permission, PermissionId)]] =
    for {
      u <- Uri.fromString(s"/$appName").leftWiden[Throwable].liftTo[F]
      req <- GET(u)
      res <- endpoints.run(req)
      perms <- res.as[SiteResult[List[(Permission, PermissionId)]]]
    } yield perms.result
}
