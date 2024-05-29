package h4sm.featurerequests
package infrastructure
package endpoint

import cats.effect.{Bracket, Sync}
import cats.syntax.all._
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec._
import h4sm.auth.{UserAuthService, UserId}
import h4sm.auth.domain.tokens.AsBaseToken
import h4sm.featurerequests.comm.codecs._
import h4sm.featurerequests.comm.domain.votes._
import h4sm.featurerequests.infrastructure.repository.persistent.VoteRepositoryInterpreter
import comm.domain.votes.VoteRequest
import domain.votes.VoteRepositoryAlgebra
import doobie.util.transactor.Transactor
import tsec.authentication._

class VoteEndpoints[F[_]: Sync: VoteRepositoryAlgebra, T[_]](implicit T: AsBaseToken[T[UserId]])
    extends Http4sDsl[F] {
  def submitVote: UserAuthService[F, T] =
    UserAuthService { case req @ POST -> Root / "vote" asAuthed _ =>
      for {
        voteReq <- req.request.as[VoteRequest]
        vote = Vote(
          voteReq.featureRequest,
          T.asBase(req.authenticator).identity.some,
          voteReq.vote,
          voteReq.comment,
        )
        _ <- VoteRepositoryAlgebra[F].insert(vote)
        resp <- Ok()
      } yield resp
    }

  def endpoints: UserAuthService[F, T] = submitVote
}

object VoteEndpoints {
  def persistingEndpoints[F[_]: Sync: Bracket[?[_], Throwable], T[_]](
      xa: Transactor[F],
  )(implicit
      T: AsBaseToken[T[UserId]],
  ): VoteEndpoints[F, T] = {
    implicit val voteRepo = new VoteRepositoryInterpreter(xa)
    new VoteEndpoints[F, T]
  }
}
