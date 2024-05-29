package h4sm.db

import cats.{Applicative, ApplicativeError, Functor}
import cats.syntax.all._
import cats.mtl.{ApplicativeAsk, DefaultApplicativeAsk}
import io.circe.Decoder
import io.circe.config.parser
import io.circe.generic.semiauto._

package object config {
  implicit val dbConfigDecoder: Decoder[DatabaseConfig] = deriveDecoder

  private def pureConfigAsk[F[_], C: Decoder](name: Option[String])(implicit
      ev: ApplicativeError[F, Throwable],
  ): ApplicativeAsk[F, C] =
    new DefaultApplicativeAsk[F, C] {
      val c: F[C] = name.fold(parser.decodeF[F, C]())(parser.decodePathF[F, C](_))
      val applicative: Applicative[F] = implicitly
      def ask: F[C] = c
    }

  def getPureConfigAsk[F[_], C: Decoder](implicit ev: ApplicativeError[F, Throwable]) =
    pureConfigAsk[F, C](None)

  def getPureConfigAskPath[F[_], C: Decoder](
      path: String,
  )(implicit ev: ApplicativeError[F, Throwable]) =
    pureConfigAsk[F, C](path.some)

  implicit def ConfigAskFunctor[F[_]: Functor] =
    new Functor[ApplicativeAsk[F, ?]] {
      def map[AA, B](fa: ApplicativeAsk[F, AA])(f: AA => B) =
        new DefaultApplicativeAsk[F, B] {
          val applicative: Applicative[F] = fa.applicative
          def ask: F[B] = fa.ask.map(f)
        }
    }
}
