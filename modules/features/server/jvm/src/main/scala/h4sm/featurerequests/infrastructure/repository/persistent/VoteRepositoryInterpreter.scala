package h4sm
package featurerequests
package infrastructure.repository.persistent

import domain.votes._
import cats.data.OptionT
import cats.effect.Bracket
import cats.syntax.all._
import doobie._
import doobie.implicits._
import doobie.postgres.syntax.monaderror._
import db.sql._
import h4sm.featurerequests.comm.domain.VoteId
import h4sm.featurerequests.comm.domain.votes.Vote

class VoteRepositoryInterpreter[M[_]: Bracket[?[_], Throwable]](val xa: Transactor[M])
    extends VoteRepositoryAlgebra[M] {
  def getVote(vote: Vote): M[Option[(VoteId, Vote)]] = votes.select(vote).option.transact(xa)

  def insert(a: Vote): M[Unit] =
    votes.insert(a).run.onUniqueViolation(votes.update(a).run).transact(xa).as(())

  def insertGetId(a: Vote): OptionT[M, VoteId] =
    OptionT {
      votes.insertGetId(a).map(_.some).onUniqueViolation(HC.rollback.as(none[VoteId])).transact(xa)
    }
}
