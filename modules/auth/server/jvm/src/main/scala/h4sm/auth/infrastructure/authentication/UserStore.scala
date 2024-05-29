package h4sm.auth
package infrastructure
package authentication

import cats.Monad
import cats.data.OptionT
import cats.syntax.all._
import tsec.common.SecureRandomId
import h4sm.auth.domain.users.UserRepositoryAlgebra
import tsec.authentication._
import db.domain._
import domain.tokens.{AsBaseToken, BaseTokenReader, TokenRepositoryAlgebra}
import domain.tokens.AsBaseToken.ops._

trait FunctionKK[X[_[_]], Y[_[_]]] {
  def apply[M[_]: Monad](xa: X[M]): Y[M]
}

object FunctionKK {
  type ~~>[X[_[_]], Y[_[_]]] = FunctionKK[X, Y]
}

import FunctionKK._

trait UserBackingStore[F[_]] extends BackingStore[F, UserId, User]
trait TokenBackingStore[F[_], T[_]] extends BackingStore[F, SecureRandomId, T[UserId]]

object TransBackingStore {
  def tokenTrans[T[_]](implicit b: AsBaseToken[T[UserId]], r: BaseTokenReader[T[UserId]]) =
    new (TokenRepositoryAlgebra ~~> TokenBackingStore[?[_], T]) {
      def apply[M[_]: Monad](alg: TokenRepositoryAlgebra[M]): TokenBackingStore[M, T] =
        new TokenBackingStore[M, T] {
          def put(elem: T[UserId]): M[T[UserId]] = alg.insert(elem.asBase).as(elem)
          def update(v: T[UserId]): M[T[UserId]] = alg.updateUnique(v.asBase).as(v)
          def delete(id: SecureRandomId): M[Unit] = alg.delete(id)
          def get(id: SecureRandomId): OptionT[M, T[UserId]] = alg.byId(id).map(x => r.read(x._1))
        }
    }

  val userTrans = new (UserRepositoryAlgebra ~~> UserBackingStore) {
    def apply[M[_]: Monad](alg: UserRepositoryAlgebra[M]): UserBackingStore[M] =
      new UserBackingStore[M] {
        def put(elem: User): M[User] = alg.insert(elem).as(elem)
        def get(id: UserId): OptionT[M, User] = alg.byId(id).map(_._1)
        def update(v: User): M[User] = alg.updateUnique(v).as(v)
        def delete(id: UserId): M[Unit] = alg.delete(id)
      }
  }
}
