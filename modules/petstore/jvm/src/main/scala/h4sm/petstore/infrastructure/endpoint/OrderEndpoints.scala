package h4sm.petstore
package infrastructure.endpoint

import cats.effect.Sync
import cats.syntax.all._
import domain._
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec._
import tsec.authentication._
import h4sm.auth._
import h4sm.auth.domain.tokens._
import h4sm.petstore.infrastructure.endpoint.codecs._
import AsBaseToken.ops._

class OrderEndpoints[F[_]: Sync: OrderAlgebra, T[_]](auth: UserSecuredRequestHandler[F, T])(implicit
    baseToken: AsBaseToken[T[UserId]],
) extends Http4sDsl[F] {
  def createOrder =
    UserAuthService[F, T] { case req @ POST -> Root asAuthed _ =>
      for {
        order <- req.request.as[OrderRequest]
        _ <- OrderAlgebra[F].insert(Order(order.petId, req.authenticator.asBase.identity))
        res <- Ok()
      } yield res
    }

  val authService = createOrder

  val endpoints = auth.liftService(createOrder)
}
