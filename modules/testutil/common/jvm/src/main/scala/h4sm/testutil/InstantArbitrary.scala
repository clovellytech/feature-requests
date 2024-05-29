package h4sm.testutil

import cats.syntax.all._
import java.time.Instant
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.cats.implicits._

trait InstantArbitrary {
  val instant = (
    Gen.chooseNum(0L, Instant.MAX.getEpochSecond),
    Gen.chooseNum(0L, Instant.MAX.getNano.toLong),
  ).mapN(Instant.ofEpochMilli(_).plusNanos(_))

  implicit val arbInstant: Arbitrary[Instant] = Arbitrary(instant)
}
