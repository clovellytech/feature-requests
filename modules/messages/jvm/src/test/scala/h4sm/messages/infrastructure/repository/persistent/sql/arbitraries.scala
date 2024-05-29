package h4sm.messages.infrastructure.repository.persistent.sql

import cats.syntax.all._
import org.scalacheck._
import org.scalacheck.cats.implicits._
import h4sm.messages.domain._
import h4sm.testutil.arbitraries._
import h4sm.messages.infrastructure.endpoint.CreateMessageRequest

object arbitraries {
  implicit val createMessageArb: Arbitrary[CreateMessageRequest] = Arbitrary {
    (
      Gen.uuid,
      nonEmptyString,
    ).mapN(CreateMessageRequest.apply _)
  }

  implicit val messageArb: Arbitrary[UserMessage] = Arbitrary {
    (
      Gen.uuid,
      Gen.uuid,
      nonEmptyString,
      Gen.option(arbInstant.arbitrary),
    ).mapN(UserMessage.apply _)
  }
}
