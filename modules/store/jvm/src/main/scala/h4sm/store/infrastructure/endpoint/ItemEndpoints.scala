package h4sm.store
package infrastructure.endpoint

import cats.effect.Sync
import cats.syntax.all._
import domain._
import h4sm.auth._
import h4sm.auth.domain.tokens._
import h4sm.auth.domain.tokens.AsBaseToken.ops._
import h4sm.store.infrastructure.endpoint.codecs._
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec._
import tsec.authentication._

class ItemEndpoints[F[_]: Sync: ItemAlgebra: OrderAlgebra, T[_]](
    auth: UserSecuredRequestHandler[F, T],
)(implicit
    B: AsBaseToken[T[UserId]],
) extends Http4sDsl[F] {
  def addItem =
    UserAuthService[F, T] { case req @ POST -> Root asAuthed _ =>
      for {
        pr <- req.request.as[ItemRequest]
        _ <- ItemAlgebra[F].insert(
          Item(pr.name, pr.description, req.authenticator.asBase.identity, pr.price),
        )
        res <- Ok()
      } yield res
    }

  def listItems =
    UserAuthService[F, T] { case GET -> Root asAuthed _ =>
      for {
        items <- ItemAlgebra[F].select
        res <- Ok(items)
      } yield res
    }

  val authService = addItem <+> listItems

  val endpoints = auth.liftService(authService)
}
