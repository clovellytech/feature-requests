package h4sm.petstore.infrastructure.endpoint

import cats.syntax.all._
import h4sm.testutil.arbitraries._
import org.scalacheck._
import org.scalacheck.cats.implicits._

object arbitraries {
  implicit val petReqArb: Arbitrary[PetRequest] = Arbitrary(
    (
      nonEmptyString,
      Gen.option(nonEmptyString),
      nonEmptyString,
    ).mapN(PetRequest.apply _),
  )
}
