package h4sm.auth
package domain

import cats.MonadError
import cats.syntax.all._
import h4sm.auth.db.domain._
import h4sm.auth.domain.users._
import tsec.common._
import tsec.passwordhashers.jca.JCAPasswordPlatform
import tsec.passwordhashers.{PasswordHash, PasswordHasher}

class UserService[F[_]: MonadError[?[_], Throwable]: UserRepositoryAlgebra, A](
    hasher: JCAPasswordPlatform[A],
)(implicit P: PasswordHasher[F, A]) {
  def signupUser(username: String, plainPassword: String): F[(User, UserId)] =
    for {
      foundUser <- UserRepositoryAlgebra[F].byUsername(username).isDefined
      _ <- if (foundUser) Error.Duplicate().raiseError[F, Throwable] else ().pure[F]
      hash <- hasher.hashpw[F](plainPassword.getBytes())
      user = User(username, hash.getBytes)
      uid <-
        UserRepositoryAlgebra[F]
          .insertGetId(user)
          .toRight[Throwable](Error.Duplicate())
          .rethrowT
    } yield (user, uid)

  def lookup(username: String, password: String): F[(User, UserId)] =
    for {
      (user, userId, joinTime) <-
        UserRepositoryAlgebra[F]
          .byUsername(username)
          .toRight[Throwable](Error.NotFound())
          .rethrowT
      hash = PasswordHash[A](new String(user.hash))
      status <- hasher.checkpw[F](password.getBytes, hash)
      res <-
        if (status == Verified) (user, userId).pure[F]
        else MonadError[F, Throwable].raiseError(Error.BadLogin())
    } yield res

  def byUserId(userId: UserId): F[(User, UserId, Instant)] =
    UserRepositoryAlgebra[F].byId(userId).toRight[Throwable](Error.NotFound()).rethrowT

  def byUsername(username: String): F[(User, UserId, Instant)] =
    UserRepositoryAlgebra[F].byUsername(username).toRight[Throwable](Error.NotFound()).rethrowT
}
