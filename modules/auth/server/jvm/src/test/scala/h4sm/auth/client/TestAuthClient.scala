package h4sm.auth
package client

import cats.effect.{IO, Sync}
import cats.syntax.all._
import h4sm.auth.comm.UserRequest
import org.http4s.Headers
import org.scalacheck.Arbitrary
import org.scalatest.Assertion
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class TestAuthClient[F[_]: Sync, Alg, T[_]](client: AuthClient[F, Alg, T]) {
  def withUser[A](u: UserRequest)(f: Headers => F[A]): F[A] =
    for {
      _ <- client.postUser(u)
      login <- client.loginUser(u)
      result <- f(client.getAuthHeaders(login))
      _ <- client.deleteUser(u.username)
    } yield result
}

trait IOTestAuthClientChecks { this: ScalaCheckPropertyChecks =>
  def forAnyUser[Alg, T[_]](
      tc: TestAuthClient[IO, Alg, T],
  )(f: Headers => UserRequest => IO[Assertion])(implicit
      arb: Arbitrary[UserRequest],
  ): Assertion =
    forAll { (u: UserRequest) =>
      tc.withUser(u)(headers => f(headers)(u)).unsafeRunSync()
    }

  def forAnyUser2[A: Arbitrary, Alg, T[_]](
      tc: TestAuthClient[IO, Alg, T],
  )(f: Headers => (UserRequest, A) => IO[Assertion])(implicit
      arb: Arbitrary[UserRequest],
  ): Assertion =
    forAll { (u: UserRequest, a: A) =>
      tc.withUser(u)(headers => f(headers)(u, a)).unsafeRunSync()
    }

  def forAnyUser3[A: Arbitrary, B: Arbitrary, Alg, T[_]](
      tc: TestAuthClient[IO, Alg, T],
  )(f: Headers => (UserRequest, A, B) => IO[Assertion])(implicit
      arb: Arbitrary[UserRequest],
  ): Assertion = forAnyUser2(tc)(h => (u: UserRequest, x: (A, B)) => f(h)(u, x._1, x._2))
}
