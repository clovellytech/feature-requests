package h4sm.store
package infrastructure.endpoint

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.all._
import domain._
import h4sm.auth._
import h4sm.auth.domain.tokens._
import h4sm.store.infrastructure.endpoint.codecs._
import AsBaseToken.ops._
import java.util.UUID
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec._
import tsec.authentication._

sealed abstract class OrderError extends Throwable
final case class BadOrder() extends OrderError
final case class NoOrder() extends OrderError

class OrderEndpoints[F[_]: Sync: OrderAlgebra: ItemAlgebra, T[_]](
    auth: UserSecuredRequestHandler[F, T],
)(implicit
    baseToken: AsBaseToken[T[UserId]],
) extends Http4sDsl[F] {
  def createOrder =
    UserAuthService[F, T] { case req @ POST -> Root asAuthed _ =>
      val result = for {
        orderReq <- req.request.as[OrderRequest]
        items <- ItemAlgebra[F].byIds(orderReq.items.map(_._1).toList)
        orderItems = items.map { case (Item(_, _, _, price), itemId, _) =>
          OrderItem(itemId, orderReq.items(itemId), price)
        }
        order = Order(req.authenticator.asBase.identity, orderItems, None, None, 100)
        oid <- OrderAlgebra[F].insertGetId(order).toRight[Throwable](BadOrder()).rethrowT
        _ <- orderItems.traverse(OrderAlgebra[F].insertOrderItem(oid, _)).void
        res <- Ok()
      } yield res

      result.recoverWith { case BadOrder() =>
        Conflict("Conflicting order")
      }
    }

  val viewOrder = UserAuthService[F, T] { case req @ GET -> Root / uuidStr asAuthed _ =>
    val result = for {
      orderId <- EitherT.fromEither[F](Either.catchNonFatal(UUID.fromString(uuidStr))).rethrowT
      order <- OrderAlgebra[F].byId(orderId).toRight[Throwable](NoOrder()).rethrowT
      _ <-
        if (order._1.createBy.compareTo(req.authenticator.asBase.identity) == 0) ().pure[F]
        else NoOrder().raiseError[F, Unit]
      res <- Ok()
    } yield res

    result.recoverWith { case NoOrder() =>
      NotFound()
    }
  }

  val submitOrder = UserAuthService[F, T] { case req @ POST -> Root / "submit" asAuthed _ =>
    for {
      orderReq <- req.request.as[ViewOrderRequest]
      (_, orderId, _) <-
        OrderAlgebra[F]
          .byId(orderReq.orderId)
          .toRight[Throwable](NoOrder())
          .rethrowT
      _ <- OrderAlgebra[F].submitOrder(orderId)
      res <- Ok()
    } yield res
  }

  val authService = createOrder <+> viewOrder <+> submitOrder

  val endpoints = auth.liftService(authService)
}
