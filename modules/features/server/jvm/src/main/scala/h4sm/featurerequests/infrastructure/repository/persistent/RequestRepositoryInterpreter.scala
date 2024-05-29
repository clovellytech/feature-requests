package h4sm.featurerequests
package infrastructure.repository.persistent

import java.time.Instant

import cats.effect.Bracket
import cats.data.OptionT
import cats.syntax.all._
import doobie._
import doobie.implicits._
import doobie.postgres.syntax.monaderror._
import db.sql._
import domain.requests._
import h4sm.featurerequests.comm.domain.features.Feature
import h4sm.featurerequests.comm.domain.votes.VotedFeature
import h4sm.featurerequests.comm.domain.featureIdTypes._

class RequestRepositoryInterpreter[M[_]: Bracket[?[_], Throwable]](xa: Transactor[M])
    extends RequestRepositoryAlgebra[M] {
  def insert(r: Feature): M[Unit] = requests.insert(r).run.as(()).transact(xa)

  def select: M[List[(Feature, FeatureId, Instant)]] = requests.select.to[List].transact(xa)

  def byId(id: FeatureId): OptionT[M, (Feature, FeatureId, Instant)] =
    OptionT(requests.selectById(id).option.transact(xa))

  def insertGetId(a: Feature): OptionT[M, FeatureId] =
    OptionT {
      requests
        .insertGetId(a)
        .map(_.some)
        .onUniqueViolation {
          HC.rollback.as(none[FeatureId])
        }
        .transact(xa)
    }

  def selectWithVoteCounts: M[List[VotedFeature]] =
    requests.selectAllWithVoteCounts.to[List].transact(xa)
}
