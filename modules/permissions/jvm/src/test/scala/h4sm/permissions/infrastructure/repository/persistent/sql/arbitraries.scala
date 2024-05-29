package h4sm
package permissions.infrastructure.repository.persistent.sql

import java.util.UUID

import cats.syntax.all._
import h4sm.permissions.domain._
import org.scalacheck._
import org.scalacheck.cats.implicits._
import testutil.arbitraries._

object arbitraries {
  implicit val uuidArb: Arbitrary[UUID] = Arbitrary(Gen.uuid)

  implicit val arbPermission: Arbitrary[Permission] = Arbitrary {
    (
      nonEmptyString,
      nonEmptyString,
      nonEmptyString,
    ).mapN(Permission.apply _)
  }

  implicit val userPermissionArb: Arbitrary[UserPermission[PermissionId]] = Arbitrary {
    (
      Gen.uuid,
      Gen.uuid,
      Gen.uuid,
    ).mapN(UserPermission.apply _)
  }
}
