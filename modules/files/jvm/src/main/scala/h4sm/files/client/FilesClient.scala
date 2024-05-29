package h4sm
package files
package client

import java.io.File

import cats.syntax.all._
import cats.effect._
import files.domain._
import files.infrastructure.endpoint._
import fs2.Stream
import h4sm.auth.comm.SiteResult
import h4sm.auth.comm.codecs._
import h4sm.files.domain.FileInfo
import org.http4s._
import org.http4s.implicits._
import org.http4s.headers._
import org.http4s.dsl._
import org.http4s.client.dsl._
import org.http4s.multipart._
import org.http4s.circe.CirceEntityCodec._

import testutil.infrastructure.endpoints._

class FilesClient[F[_]: ContextShift, T[_]](fileEndpoints: FileEndpoints[F, T])(implicit
    F: Sync[F],
    blk: Blocker,
) extends Http4sDsl[F]
    with Http4sClientDsl[F] {
  val codecs = new FileCodecs[F]
  import codecs._

  val files = fileEndpoints.endpoints.orNotFound

  def postFile(fileInfo: FileInfo, file: File)(implicit
      h: Headers,
  ): F[SiteResult[List[FileInfoId]]] = {
    val mp: Multipart[F] = Multipart(
      Vector(
        Part.fileData(
          fileInfo.name.getOrElse("file"),
          file,
          blk,
          `Content-Type`(MediaType.text.plain),
        ),
      ),
    )
    for {
      req <- POST(mp, Uri.uri("/"), h.toList ++ mp.headers.toList: _*)
      resp <- files.run(req)
      _ <- passOk(resp)
      fileRes <- resp.as[SiteResult[List[FileInfoId]]]
    } yield fileRes
  }

  def getFile(fileId: FileInfoId, name: String = "download")(implicit
      h: Headers,
  ): F[Stream[F, Byte]] =
    for {
      u <- Uri.fromString(s"/${fileId.toString}/$name").leftWiden[Throwable].liftTo[F]
      req <- GET(u)
      resp <- files.run(req.withHeaders(h))
      _ <- passOk(resp)
    } yield resp.body

  def listFiles()(implicit h: Headers): F[SiteResult[List[(FileInfoId, FileInfo)]]] =
    for {
      req <- GET(Uri.uri("/"))
      hreq = req.withHeaders(h)
      resp <- files.run(hreq)
      _ <- passOk(resp)
      fileInfo <- resp.as[SiteResult[List[(FileInfoId, FileInfo)]]]
    } yield fileInfo
}
