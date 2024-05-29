package h4sm.auth.client

import cats.syntax.all._
import h4sm.testutil.arbitraries._
import h4sm.auth.comm._
import org.scalacheck._
import org.scalacheck.cats.implicits._

object arbitraries {
  implicit val UserRequestArb: Arbitrary[UserRequest] = Arbitrary {
    (
      nonEmptyString,
      nonEmptyString,
    ).mapN(UserRequest.apply _)
  }
}
