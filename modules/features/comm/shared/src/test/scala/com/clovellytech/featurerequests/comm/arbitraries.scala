package h4sm.featurerequests.comm

import cats.syntax.all._
import h4sm.featurerequests.comm.domain.features.FeatureRequest
import h4sm.testutil.arbitraries._
import org.scalacheck.Arbitrary
import org.scalacheck.cats.implicits._

object arbitraries {
  implicit val featureRequest: Arbitrary[FeatureRequest] = Arbitrary {
    (
      nonEmptyString,
      nonEmptyString,
    ).mapN(FeatureRequest.apply _)
  }
}
