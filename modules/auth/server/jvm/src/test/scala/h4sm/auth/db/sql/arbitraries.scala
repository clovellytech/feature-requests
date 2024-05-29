package h4sm.auth
package db.sql

import cats.syntax.all._
import h4sm.auth.db.domain.User
import h4sm.auth.domain.tokens.BaseToken
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.cats.implicits._
import h4sm.testutil.arbitraries._
import tsec.common.SecureRandomId

object arbitraries {
  implicit val userIdArb: Arbitrary[UserId] = Arbitrary(Gen.uuid)

  implicit val secureRandomIdArb: Arbitrary[SecureRandomId] = Arbitrary {
    Gen.posNum[Int].map(num => SecureRandomId(num.toString))
  }

  implicit val baseTokenArb: Arbitrary[BaseToken] = Arbitrary {
    (
      secureRandomIdArb.arbitrary,
      Gen.uuid,
      arbInstant.arbitrary,
      Gen.option(arbInstant.arbitrary),
    ).mapN(BaseToken.apply _)
  }

  implicit val userArb: Arbitrary[User] = Arbitrary {
    (
      nonEmptyString,
      Gen.listOf(Gen.choose(Byte.MinValue, Byte.MaxValue)).map(_.toArray),
    ).mapN(User.apply _)
  }
}
