package h4sm.featurerequests.client

import cats.data.StateT
import h4sm.auth.client.implicits._
import h4sm.auth.client.{Client => AuthClient}
import h4sm.auth.comm.arbitraries._
import h4sm.auth.comm.UserRequest
import h4sm.featurerequests.comm.arbitraries._
import h4sm.featurerequests.comm.domain.features.FeatureRequest
import org.scalacheck._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext

class FeatureRequestClientTestSpec extends AsyncFlatSpec with Matchers {
  implicit override def executionContext = JSExecutionContext.Implicits.queue

  val authClient = new AuthClient[Future] {
    override def base: String = "http://localhost:8080/users"
  }

  val featuresClient = new FeatureRequestClient[Future] {
    override def base: String = "http://localhost:8080/requests"
  }

  "A user" should "be able to add a feature" in {
    // forAll and futures don't mix...
    val (u, r) = implicitly[Arbitrary[(UserRequest, FeatureRequest)]].arbitrary.sample.get

    val t = for {
      _ <- StateT.liftF(authClient.signup(u))
      _ <- authClient.getSession(u)
      _ <- featuresClient.postRequest(r)
      fs <- StateT.liftF(featuresClient.getRequests)
      _ <- StateT.liftF(authClient.delete(u.username))
    } yield fs.map(rr => FeatureRequest(rr.feature.title, rr.feature.description)) should contain(r)

    t.runEmptyA
  }
}
