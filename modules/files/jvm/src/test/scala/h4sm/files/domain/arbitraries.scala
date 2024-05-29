package h4sm.files.domain

import java.util.UUID

import cats.syntax.all._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.cats.implicits._
import h4sm.testutil.arbitraries.nonEmptyString
import h4sm.files.infrastructure.endpoint.FileUpload

object arbitraries {
  implicit val backends: Gen[Backend] = Gen.oneOf(Seq(Backend.LocalBackend))

  implicit val backendArb: Arbitrary[Backend] = Arbitrary(backends)

  implicit val uuidArb: Arbitrary[UUID] = Arbitrary(Gen.uuid)

  implicit val fileInfoGen: Gen[FileInfo] = (
    Gen.option(nonEmptyString),
    Gen.option(nonEmptyString),
    Gen.option(nonEmptyString),
    Gen.option(nonEmptyString),
    Gen.uuid,
    Gen.oneOf(true, false),
    backends,
  ).mapN(FileInfo.apply _)

  implicit val fileInfoArb: Arbitrary[FileInfo] = Arbitrary(fileInfoGen)

  implicit val fileUploadArb: Arbitrary[FileUpload] = Arbitrary(
    (
      nonEmptyString,
      Gen.option(nonEmptyString),
      Gen.oneOf(true, false),
    ).mapN(FileUpload.apply _),
  )
}
