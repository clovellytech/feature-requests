package h4sm
package permissions
package infrastructure.endpoint

import cats.effect.Sync
import cats.syntax.all._
import h4sm.auth.comm.SiteResult
import h4sm.auth.comm.codecs._
import h4sm.auth.{UserAuthService, UserId, UserSecuredRequestHandler}
import h4sm.auth.domain.tokens.AsBaseToken
import h4sm.permissions.domain.{Permission, PermissionAlgebra, UserPermissionAlgebra}
import h4sm.permissions.infrastructure.endpoint.codecs._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec._
import tsec.authentication._

class PermissionEndpoints[F[_]: Sync: UserPermissionAlgebra: PermissionAlgebra, T[_]](
    auth: UserSecuredRequestHandler[F, T],
)(implicit b: AsBaseToken[T[UserId]])
    extends Http4sDsl[F] {
  def createEndpoint: UserAuthService[F, T] =
    PermissionedRoutes("ct_permissions" -> "admin") { case req @ POST -> Root asAuthed _ =>
      for {
        perm <- req.request.as[Permission]
        _ <- PermissionAlgebra[F].insert(perm)
        result <- Ok()
      } yield result
    }

  def listEndpoint: UserAuthService[F, T] =
    PermissionedRoutes("permissions" -> "view") { case GET -> Root asAuthed _ =>
      for {
        perms <- PermissionAlgebra[F].select
        sendPerms = perms.map { case (perm, permid, _) => (perm, permid) }
        res <- Ok(SiteResult(sendPerms))
      } yield res
    }

  def selectByAppEndpoint: UserAuthService[F, T] =
    PermissionedRoutes("permissions" -> "view") { case GET -> Root / appName asAuthed _ =>
      for {
        perms <- PermissionAlgebra[F].selectByAppName(appName)
        res <- Ok(SiteResult(perms))
      } yield res
    }

  def authServices: UserAuthService[F, T] =
    List(
      createEndpoint,
      listEndpoint,
      selectByAppEndpoint,
    ).reduce(_ <+> _)

  def endpoints: HttpRoutes[F] = auth.liftService(authServices)
}
