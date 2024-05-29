package h4sm
package testutil.infrastructure.endpoints

import org.http4s._
import cats.effect.Sync
import cats.syntax.all._

sealed abstract class ClientError extends Throwable with Product with Serializable
final case class UriError(message: String) extends ClientError
final case class CommunicationError(status: Status, message: String) extends ClientError

trait ClientErrors {
  def commError(status: Status): Throwable = CommunicationError(status, "Error, status was not Ok")
  def uriError(message: String): Throwable = UriError(message)

  def passOk[F[_]: Sync, A](response: Response[F]): F[Response[F]] =
    response.status match {
      case Status.Ok => response.pure[F]
      case _ => commError(response.status).raiseError[F, Response[F]]
    }
}
