package h4sm.featurerequests
package infrastructure.endpoint

import cats.effect.{Bracket, Sync}
import cats.syntax.all._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import tsec.authentication._
import domain.requests._
import doobie.util.transactor.Transactor
import org.http4s.circe.CirceEntityCodec._
import h4sm.auth._
import h4sm.auth.comm.codecs._
import h4sm.auth.comm.SiteResult
import h4sm.featurerequests.comm.codecs._
import h4sm.featurerequests.infrastructure.repository.persistent.RequestRepositoryInterpreter
import h4sm.featurerequests.comm.domain.features.{Feature, FeatureRequest}
import h4sm.auth.domain.tokens.AsBaseToken

class RequestEndpoints[F[_]: Sync: RequestRepositoryAlgebra, T[_]](implicit
    T: AsBaseToken[T[UserId]],
) extends Http4sDsl[F] {
  def unAuthEndpoints: HttpRoutes[F] =
    HttpRoutes.of[F] { case GET -> Root / "request" =>
      for {
        feats <- RequestRepositoryAlgebra[F].selectWithVoteCounts
        resp <- Ok(SiteResult(feats))
      } yield resp
    }

  def authEndpoints: UserAuthService[F, T] =
    UserAuthService { case req @ POST -> Root / "request" asAuthed _ =>
      for {
        featureRequest <- req.request.as[FeatureRequest]
        feature = Feature(
          T.asBase(req.authenticator).identity.some,
          featureRequest.title,
          featureRequest.description,
        )
        _ <- RequestRepositoryAlgebra[F].insert(feature)
        resp <- Ok()
      } yield resp
    }
}

object RequestEndpoints {
  def persistingEndpoints[F[_]: Sync: Bracket[?[_], Throwable], T[_]](
      xa: Transactor[F],
  )(implicit
      T: AsBaseToken[T[UserId]],
  ): RequestEndpoints[F, T] = {
    implicit val requestRepo = new RequestRepositoryInterpreter(xa)
    new RequestEndpoints[F, T]
  }
}
