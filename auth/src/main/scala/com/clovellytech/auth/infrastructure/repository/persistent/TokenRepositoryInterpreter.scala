package com.clovellytech.auth
package infrastructure.repository.persistent

import cats.data.OptionT
import cats.Monad
import cats.implicits._
import doobie._
import doobie.implicits._
import domain.tokens.TokenRepositoryAlgebra
import db.sql.tokens

class TokenRepositoryInterpreter[M[_] : Monad](xa : Transactor[M]) extends TokenRepositoryAlgebra[M] {
  def insert(a: BearerToken): M[Unit] = tokens.insert(a).run.as(()).transact(xa)

  def delete(i: SecureRandomId): M[Unit] = tokens.delete(i).run.as(()).transact(xa)

  def safeUpdate(id: SecureRandomId, u: BearerToken): M[Unit] = tokens.update(id, u).run.as(()).transact(xa)

  def update(u: BearerToken): M[Unit] = safeUpdate(u.id, u)

  def select: M[List[(BearerToken, SecureRandomId, Unit)]] = tokens.select.map(tok =>
    (tok, tok.id, ())
  ).to[List].transact(xa)

  def byId(id: SecureRandomId): OptionT[M, (BearerToken, SecureRandomId, Unit)] =
    OptionT(tokens.byId(id).map(tok => (tok, tok.id, ())).option.transact(xa))
}
