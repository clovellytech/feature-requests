package h4sm.messages.infrastructure.endpoint

import cats.syntax.all._
import cats.effect.Sync
import h4sm.messages.domain._
import Codecs._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import tsec.authentication._
import h4sm.auth.domain.tokens.AsBaseToken
import h4sm.auth.domain.tokens.AsBaseToken.ops._
import h4sm.auth._
import org.http4s.circe.CirceEntityCodec._

class MessageEndpoints[F[_]: Sync: MessageAlgebra, T[_]](implicit T: AsBaseToken[T[UserId]])
    extends Http4sDsl[F] {

  val thread: UserAuthService[F, T] = UserAuthService {
    case req @ GET -> Root / "thread" / withUserId asAuthed _ =>
      val res: F[Response[F]] = for {
        otherUser <- MessageId.fromString[F](withUserId)
        resp <- Ok(MessageAlgebra[F].thread(req.authenticator.asBase.identity, otherUser))
      } yield resp

      res
  }

  val send: UserAuthService[F, T] = UserAuthService { case req @ POST -> Root asAuthed _ =>
    val res: F[Response[F]] = for {
      sendReq <- req.request.as[CreateMessageRequest]
      _ <- MessageAlgebra[F].insert(
        UserMessage(req.authenticator.asBase.identity, sendReq.to, sendReq.content, none),
      )
      resp <- Ok()
    } yield resp

    res
  }

  val inbox: UserAuthService[F, T] = UserAuthService {
    case req @ POST -> Root / "inbox" asAuthed _ =>
      Ok(MessageAlgebra[F].inbox(req.authenticator.asBase.identity))
  }

  def authEndpoints = thread <+> send <+> inbox
}
