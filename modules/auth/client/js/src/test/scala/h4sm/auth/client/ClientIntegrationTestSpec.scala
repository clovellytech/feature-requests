package h4sm.auth.client

import arbitraries._
import cats.data.StateT
import cats.syntax.all._
import h4sm.auth.client.implicits._
import h4sm.auth.comm.UserRequest
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalacheck.Arbitrary
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext

class ClientIntegrationTestSpec extends AsyncFlatSpec with Matchers {
  implicit override def executionContext = JSExecutionContext.Implicits.queue

  val authClient = new Client[Future] {
    override def base: String = "http://localhost:8080/users"
  }

  "the auth client" should "be in test" in {
    authClient.isTest.map(assert(_))
  }

  it should "allow login after signup" in {
    val user = implicitly[Arbitrary[UserRequest]].arbitrary.sample.get

    val state = for {
      _ <- StateT.liftF(authClient.signup(user))
      _ <- authClient.getSession(user)
      p <- authClient.currentUser
      _ <- authClient.logout
      p2 <- authClient.currentUser.map(_.some).recover { case _ => none }
      _ <- StateT.liftF(authClient.delete(user.username))
    } yield {
      p.username should equal(user.username)
      p2 should equal(none)
    }

    state.runEmptyA
  }
}
