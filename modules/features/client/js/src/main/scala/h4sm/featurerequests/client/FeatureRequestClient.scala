package h4sm.featurerequests.client

import cats.data.StateT
import cats.Monad
import cats.syntax.all._
import h4sm.auth.comm.SiteResult
import h4sm.auth.comm.codecs._
import h4sm.common.API
import h4sm.featurerequests.comm.codecs._
import h4sm.featurerequests.comm.domain.features._
import h4sm.featurerequests.comm.domain.votes._

class FeatureRequestClient[F[_]: Monad](implicit val F: API[F]) {
  def base: String = ""
  def requestRoute: String = s"$base/request"
  def voteRoute: String = s"$base/vote"

  type Session[A] = StateT[F, F.H, A]

  def getRequests: F[List[VotedFeature]] =
    for {
      resp <- F.get(requestRoute)
      res <- resp.as[SiteResult[List[VotedFeature]]]
    } yield res.result

  def postRequest(req: FeatureRequest): Session[Unit] =
    F.postT(requestRoute, req).void

  def submitVote(req: VoteRequest): Session[Unit] =
    F.postT(voteRoute, req).void
}
