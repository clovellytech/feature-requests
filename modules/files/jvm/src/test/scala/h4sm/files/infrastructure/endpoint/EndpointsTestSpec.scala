package h4sm
package files
package infrastructure
package endpoint

import java.io.{ByteArrayOutputStream, File, PrintStream}

import cats.effect.{Blocker, IO}
import cats.syntax.all._
import doobie.implicits._
import auth.client.IOTestAuthClientChecks
import auth.comm.arbitraries._
import auth.comm.UserRequest
import files.client.FilesClientRunner
import files.domain.FileInfo
import files.db.sql.{files => filesSql}
import files.db.sql.arbitraries._
import io.circe.generic.auto._
import testutil.DbFixtureSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.ExecutionContext.Implicits.global
import cats.mtl.ApplicativeAsk
import h4sm.files.config.FileConfig

class EndpointsTestSpec
    extends DbFixtureSuite
    with Matchers
    with ScalaCheckPropertyChecks
    with IOTestAuthClientChecks {
  val schemaNames: Seq[String] = List("ct_auth", "ct_files")

  val textFile = new File(getClass.getResource("/testUpload.txt").toURI)

  implicit val c: ApplicativeAsk[IO, FileConfig] = h4sm.db.config.getPureConfigAskPath("files")
  implicit val blk: Blocker = Blocker.liftExecutionContext(global)

  test("A user with no files should be able to retrieve empty list of files") { p =>
    new FilesClientRunner[IO] {
      val xa = p.transactor

      forAnyUser(testAuthClient) { implicit headers => _ =>
        fileClient.listFiles().map(_.result should be(empty))
      }
    }
  }

  test("A user should be able to upload a file") { p =>
    new FilesClientRunner[IO] {
      def xa = p.transactor
      forAnyUser2(testAuthClient) { implicit headers => (_: UserRequest, fi: FileInfo) =>
        for {
          upload <- fileClient.postFile(fi, textFile)
          _ <- upload.result.traverse(filesSql.deleteById(_).run).transact(p.transactor)
        } yield upload.result should not be empty
      }
    }
  }

  test("A user with files should get a list of files") { p =>
    new FilesClientRunner[IO] {
      def xa = p.transactor
      forAnyUser2(testAuthClient) { implicit headers => (_: UserRequest, fi: FileInfo) =>
        for {
          upload <- fileClient.postFile(fi, textFile)
          fs <- fileClient.listFiles()
          _ <- upload.result.traverse(filesSql.deleteById(_).run).transact(p.transactor)
        } yield fs.result should not be empty
      }
    }
  }

  test("A user should get a specific file") { p =>
    new FilesClientRunner[IO] {
      def xa = p.transactor
      forAnyUser2(testAuthClient) { implicit headers => (_: UserRequest, fi: FileInfo) =>
        for {
          upload <- fileClient.postFile(fi, textFile)
          fs <- fileClient.listFiles()
          (fid, filename) = {
            val (fid, FileInfo(_, _, filename, _, _, _, _)) = fs.result.head
            (fid, filename.getOrElse("download"))
          }
          f <- fileClient.getFile(fid, filename)
          bs = {
            val bytes = f
            val bs = new ByteArrayOutputStream()
            val ps = new PrintStream(bs)
            bytes.chunks
              .through(_.evalMap(c => IO(ps.write(c.toArray))))
              .compile
              .drain
              .unsafeRunSync()
            bs
          }
          _ <- upload.result.traverse(filesSql.deleteById(_).run).transact(p.transactor)
        } yield bs.toString should not be empty
      }
    }
  }
}
