package h4sm
package testutil

import java.sql.DriverManager

import cats.effect.{Async, ContextShift, Sync}
import cats.syntax.all._
import doobie.util.transactor.Transactor
import h4sm.db.config.DatabaseConfig
import io.circe.config.parser

object transactor {
  private def createOrDropDb[F[_]](db: DatabaseConfig, word: String)(implicit F: Sync[F]): F[Unit] =
    F.delay {
      Class.forName("org.postgresql.Driver")
      val c = DriverManager.getConnection(
        s"jdbc:postgresql://${db.host}:${db.port}/",
        db.user,
        db.password,
      )
      val statement = c.createStatement
      val q = s"$word database ${db.databaseName}"
      statement.executeUpdate(q)
      c.close()
    }

  def createDb[F[_]: Sync](db: DatabaseConfig): F[Unit] = createOrDropDb(db, "create")
  def dropDb[F[_]: Sync](db: DatabaseConfig): F[Unit] = createOrDropDb(db, "drop")

  def getTransactor[F[_]: Async: ContextShift](cfg: DatabaseConfig): Transactor[F] =
    Transactor.fromDriverManager[F](
      cfg.driver, // driver classname
      cfg.url, // connect URL (driver-specific)
      cfg.user, // user
      cfg.password, // password
    )

  def getInitializedTransactor[F[_]: ContextShift](cfg: DatabaseConfig, schemaNames: String*)(
      implicit F: Async[F],
  ): F[Transactor[F]] =
    DatabaseConfig.initialize[F](cfg)(schemaNames: _*) *> F.delay(getTransactor[F](cfg))

  def getTransactorForDb[F[_]: ContextShift: Async](
      dbName: String,
      schemaNames: String*,
  ): F[Transactor[F]] =
    parser
      .decodePathF[F, DatabaseConfig]("db")
      .flatMap(cfg => getInitializedTransactor(cfg.copy(databaseName = dbName), schemaNames: _*))
}
