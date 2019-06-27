package h4sm.auth
package db.sql

import cats.syntax.option._
import h4sm.auth.db.domain.User
import h4sm.auth.domain.tokens.BaseToken
import org.scalacheck.{Arbitrary, Gen}
import h4sm.testutil.arbitraries._
import tsec.common.SecureRandomId

object arbitraries {

  implicit val userIdArb : Arbitrary[UserId] = Arbitrary(Gen.uuid)

  implicit val secureRandomIdArb : Arbitrary[SecureRandomId] = Arbitrary {
    Gen.posNum[Int].map(num => SecureRandomId(num.toString))
  }

  implicit val baseTokenArb : Arbitrary[BaseToken] = Arbitrary {
    for {
      sid <- secureRandomIdArb.arbitrary
      uuid <- Gen.uuid
      time <- arbInstant.arbitrary
      otherTime <- arbInstant.arbitrary
    } yield BaseToken(sid, uuid, time, otherTime.some)
  }

  implicit val userArb : Arbitrary[User] = Arbitrary {
    for {
      name <- nonEmptyString
      hash <- nonEmptyString
    } yield User(name, hash.getBytes)
  }
}


