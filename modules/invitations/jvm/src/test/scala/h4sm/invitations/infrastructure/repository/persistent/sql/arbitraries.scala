package h4sm
package invitations.infrastructure.repository.persistent.sql

import cats.syntax.all._
import testutil.arbitraries._
import h4sm.invitations.domain.Invitation
import org.scalacheck._
import org.scalacheck.cats.implicits._

object arbitraries {
  implicit def invitationArbitrary[A](implicit A: Arbitrary[A]): Arbitrary[Invitation[A]] =
    Arbitrary {
      (
        A.arbitrary,
        nonEmptyString,
        nonEmptyString,
        nonEmptyString,
        Gen.option(arbInstant.arbitrary),
        Gen.option(arbInstant.arbitrary),
        Gen.option(arbInstant.arbitrary),
        Gen.option(arbInstant.arbitrary),
      ).mapN(Invitation.apply[A])
    }
}
