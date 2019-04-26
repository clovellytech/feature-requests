package h4sm.files
package infrastructure
package endpoint

import java.io.{ByteArrayOutputStream, File, PrintStream}

import cats.effect.{ContextShift, IO, Sync}
import cats.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor
import h4sm.auth.client.{AuthClient, IOTestAuthClientChecks, TestAuthClient}
import h4sm.auth.infrastructure.endpoint.{AuthEndpoints, Authenticators, UserRequest}
import h4sm.auth.infrastructure.endpoint.arbitraries._
import h4sm.auth.infrastructure.repository.persistent.{TokenRepositoryInterpreter, UserRepositoryInterpreter}
import h4sm.db.config._
import h4sm.dbtesting.DbFixtureSuite
import h4sm.files.config.FileConfig
import h4sm.files.domain.FileInfo
import h4sm.files.infrastructure.backends._
import h4sm.files.db.sql.{files => filesSql}
import h4sm.files.db.sql.arbitraries._
import io.circe.config.parser
import io.circe.generic.auto._
import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tsec.authentication.TSecBearerToken
import tsec.passwordhashers.jca.BCrypt
import h4sm.auth.domain.tokens._

import scala.concurrent.ExecutionContext.Implicits.global

final case class Clients[F[_], A, T[_]](auth: TestAuthClient[F], files: FilesClient[F, T])

object Clients{
  def apply[F[_]: Sync: ContextShift](xa: Transactor[F]): Clients[F, BCrypt, TSecBearerToken] = {
    implicit val userService = new UserRepositoryInterpreter(xa)
    implicit val tokenService = new TokenRepositoryInterpreter(xa)
    val authEndpoints = new AuthEndpoints(BCrypt, Authenticators.bearer[F])
    val authClient = AuthClient.fromTransactor(xa)

    val testAuth = new TestAuthClient(authClient)

    implicit val c = getPureConfigAsk[F, FileConfig]("files")
    implicit val fileMetaBackend = new FileMetaService(xa)
    implicit val fileStoreBackend = new LocalFileStoreService[F]
    val fileEndpoints = new FileEndpoints(authEndpoints.Auth)
    val fileClient = new FilesClient(fileEndpoints)

    Clients(testAuth, fileClient)
  }
}

class EndpointsTestSpec extends DbFixtureSuite with Matchers with ScalaCheckPropertyChecks with IOTestAuthClientChecks {

  val schemaNames: Seq[String] = List("ct_auth", "ct_files")
  val config : DatabaseConfig = parser.decodePathF[IO, DatabaseConfig]("db").unsafeRunSync()

  val textFile = new File(getClass.getResource("/testUpload.txt").toURI)

  test("A user with no files should be able to retrieve empty list of files") { p =>
    val clients = Clients(p.transactor)

    forAnyUser(clients.auth) { implicit headers => _ =>
      clients.files.listFiles().map(_.result should be (empty))
    }
  }

  test("A user should be able to upload a file") { p =>
    val clients = Clients(p.transactor)

    forAnyUser2(clients.auth) { implicit headers => (_: UserRequest, fi: FileInfo) =>
      for {
        upload <- clients.files.postFile(fi, textFile)
        _ <- upload.result.traverse(filesSql.deleteById(_).run).transact(p.transactor)
      } yield {
        upload.result should not be (empty)
      }
    }
  }

  test("A user with files should get a list of files") { p =>
    val clients = Clients(p.transactor)

    forAnyUser2(clients.auth) { implicit headers => (_: UserRequest, fi: FileInfo) =>
      for {
        upload <- clients.files.postFile(fi, textFile)
        fs <- clients.files.listFiles()
        _ <- upload.result.traverse(filesSql.deleteById(_).run).transact(p.transactor)
      } yield {
        fs.result should not be empty
      }
    }
  }

  test("A user should get a specific file"){ p =>
    val clients = Clients(p.transactor)

    forAnyUser2(clients.auth) { implicit headers => (_: UserRequest, fi: FileInfo) =>
      for {
        upload <- clients.files.postFile(fi, textFile)
        fs <- clients.files.listFiles()
        (fid, filename) = {
          val (fid, FileInfo(_, _, filename, _, _, _, _)) = fs.result.head
          (fid, filename.getOrElse("download"))
        }
        f <- clients.files.getFile(fid, filename)
        bs = {
          val bytes = f
          val bs = new ByteArrayOutputStream()
          val ps = new PrintStream(bs)
          bytes.chunks.through(_.evalMap(c => IO(ps.write(c.toArray)))).compile.drain.unsafeRunSync()
          bs
        }
        _ <- upload.result.traverse(filesSql.deleteById(_).run).transact(p.transactor)
      } yield {
        bs.toString should not be empty
      }
    }
  }
}
