package h4sm
package messages
package client

import auth.comm.SiteResult
import auth.comm.codecs._
import auth.UserSecuredRequestHandler
import cats.data.Kleisli
import cats.syntax.all._
import cats.effect.Sync
import infrastructure.endpoint._
import messages.domain._
import messages.infrastructure.endpoint.Codecs._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.circe.CirceEntityCodec._
import testutil.infrastructure.endpoints._

class MessageClient[F[_]: Sync, T[_]](
    es: MessageEndpoints[F, T],
    auth: UserSecuredRequestHandler[F, T],
) extends Http4sDsl[F]
    with Http4sClientDsl[F]
    with SessionClientDsl[F] {
  val endpoints: Kleisli[F, Request[F], Response[F]] = auth.liftService(es.authEndpoints).orNotFound

  def sendMesssage(p: CreateMessageRequest)(implicit h: Headers): F[Unit] =
    for {
      req <- post(p, Uri.uri("/"))
      resp <- endpoints.run(req)
      _ <- passOk(resp)
    } yield ()

  def getInbox()(implicit h: Headers): F[List[UserMessage]] =
    for {
      req <- get(Uri.uri("/"))
      res <- endpoints.run(req)
      perms <- res.as[SiteResult[List[UserMessage]]]
    } yield perms.result
}
