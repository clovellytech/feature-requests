package h4sm
package auth
package infrastructure
package endpoint

import cats.effect.IO
import cats.syntax.all._
import h4sm.auth.comm.{SiteResult, UserDetail, UserRequest}
import h4sm.auth.comm.codecs._
import org.http4s.circe.CirceEntityCodec._
import testutil.EndpointTestSpec
import domain.UserService
import domain.tokens._
import doobie.util.transactor.Transactor
import infrastructure.repository.persistent._
import org.scalatest._
import org.http4s.Status
import auth.client.AuthClient
import tsec.authentication.TSecBearerToken
import tsec.passwordhashers.jca.BCrypt

class AuthEndpointsTestSpec extends EndpointTestSpec {
  val schemaNames: Seq[String] = List("ct_auth")

  def client(tr: Transactor[IO]): AuthClient[IO, BCrypt, TSecBearerToken] = {
    implicit val userAlg = new UserRepositoryInterpreter(tr)
    val userService = new UserService[IO, BCrypt](BCrypt)
    implicit val tokenService = new TokenRepositoryInterpreter(tr)
    new AuthClient(userService, Authenticators.bearer[IO])
  }

  val user = UserRequest("zak", "password")

  testIO("a signup request should return 200 status") { p =>
    val authClient = client(p.transactor)
    for {
      post <- authClient.postUser(user)
      _ <- authClient.deleteUser(user.username)
    } yield post.status should equal(Status.Ok)
  }

  testIO("a user exists request should return false for no user") { p =>
    val authClient = client(p.transactor)
    authClient.userExists(user.username).map(_ should equal(false))
  }

  testIO("a signed up user should show user exists") { p =>
    val authClient = client(p.transactor)
    for {
      _ <- authClient.postUser(user)
      exists <- authClient.userExists(user.username)
      _ <- authClient.deleteUser(user.username)
    } yield exists should equal(true)
  }

  testIO("a login request should return 200") { p =>
    val authClient = client(p.transactor)
    import authClient._
    for {
      _ <- postUser(user)
      login <- loginUser(user)
      _ <- deleteUser(user.username)
    } yield {
      login.status should equal(Status.Ok)
      login.headers.toList.map(_.name.toString) should contain("Authorization")
    }
  }

  testIO("a user should get a usable session on login") { p =>
    val authClient = client(p.transactor)
    import authClient._
    for {
      _ <- postUser(user)
      login <- loginUser(user)
      userResp <- getUser(user.username, login).pure[IO]
      handledResp <- userResp.fold(
        IO.raiseError(_),
        identity,
      )
      detail <- handledResp.attemptAs[SiteResult[UserDetail]].value
      _ <- deleteUser(user.username)
    } yield {
      login.status should equal(Status.Ok)
      handledResp.status should equal(Status.Ok)
      detail.fold[Assertion](
        e => fail(e),
        _.result.username should equal(user.username),
      )
    }
  }
}
