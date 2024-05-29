import cats.mtl.ApplicativeAsk
import cats.effect.Sync
import cats.syntax.all._
import h4sm.db.config._
import h4sm.files.config.FileConfig
import io.circe.generic.auto._

package object h4sm {
  type ConfigAsk[F[_]] = MainConfig.ConfigAsk[F]

  def getConfigAsk[F[_]: Sync]: ConfigAsk[F] = getPureConfigAsk[F, MainConfig]

  /*
   * Given the ConfigAskFunctor, we can easily create child instances for the various
   * components of the system we're building.
   */
  def getFileConfigAsk[F[_]: Sync]: ApplicativeAsk[F, FileConfig] = getConfigAsk[F].map(_.files)
}
