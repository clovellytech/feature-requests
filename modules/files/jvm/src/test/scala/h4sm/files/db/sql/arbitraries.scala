package h4sm.files
package db.sql

import java.util.UUID

import cats.syntax.all._
import h4sm.files.domain.{Backend, FileInfo}
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.cats.implicits._
import h4sm.testutil.arbitraries._

object arbitraries {
  implicit val backends: Gen[Backend] = Gen.oneOf(Seq(Backend.LocalBackend))

  implicit val backendArb: Arbitrary[Backend] = Arbitrary(backends)

  implicit val uuidArb: Arbitrary[UUID] = Arbitrary(Gen.uuid)

  implicit val fileInfoArb: Arbitrary[FileInfo] = Arbitrary {
    (
      Gen.option(nonEmptyString),
      Gen.option(nonEmptyString),
      Gen.option(nonEmptyString),
      Gen.option(nonEmptyString),
      Gen.uuid,
      Gen.oneOf(true, false),
      backends,
    ).mapN(FileInfo.apply _)
  }
}
