package h4sm.db.config

import scala.util.Try
import cats.effect.Sync
import cats.syntax.all._
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException

final case class DatabaseConfig(
    host: String,
    port: Int,
    user: String,
    password: String,
    databaseName: String,
) {
  def driver: String = "org.postgresql.Driver"
  def url: String = s"jdbc:postgresql://$host:$port/$databaseName"
}

object DatabaseConfig {

  /** Runs the flyway migrations against the target database
    */
  def initializeDb[M[_]: Sync](ds: DataSource)(schemaName: String): M[Try[Unit]] =
    Sync[M].delay {
      val fw =
        Flyway
          .configure()
          .dataSource(ds)
          .defaultSchema(schemaName)
          .schemas(schemaName)
          .locations(s"db/$schemaName/migration")
          .load()
      Try {
        fw.migrate()
        ()
      }.recoverWith { case e: FlywayException =>
        println("Got flyway exception")
        println(e)
        println("Attempting to recover.")
        Try {
          fw.repair()
          fw.migrate()
          ()
        }.recover { case e: FlywayException =>
          println("Recovery failed")
          println(e)
          ()
        }
      }
    }

  def getDataSource(cfg: DatabaseConfig): DataSource = {
    val ds = new org.postgresql.ds.PGSimpleDataSource()
    ds.setURL(cfg.url)
    ds.setUser(cfg.user)
    ds.setPassword(cfg.password)
    ds
  }

  def initialize[F[_]](cfg: DatabaseConfig)(schemaNames: String*)(implicit F: Sync[F]): F[Unit] =
    for {
      ds <- F.delay(getDataSource(cfg))
      _ <- schemaNames.toList.traverse(name => initializeDb(ds)(name))
      _ <- F.delay(ds.getConnection.close)
    } yield ()
}
