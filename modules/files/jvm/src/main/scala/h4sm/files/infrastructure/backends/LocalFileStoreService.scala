package h4sm.files
package infrastructure.backends

import java.io.File

import cats.effect.{Blocker, ContextShift, Sync}
import cats.syntax.all._
import h4sm.files.config.ConfigAsk
import h4sm.files.domain._
import fs2.Stream

class LocalFileStoreService[F[_]: Sync: ContextShift](implicit
    C: ConfigAsk[F],
    blk: Blocker,
) extends FileStoreAlgebra[F] {
  def retrieveFile(fileId: FileInfoId): F[File] =
    C.ask.map(c => new java.io.File(c.basePath, fileId.toString))

  def retrieve(fileId: FileInfoId): Stream[F, Byte] =
    for {
      file <- Stream.eval(retrieveFile(fileId))
      content <- fs2.io.file.readAll[F](file.toPath, blk, 8196)
    } yield content

  def write(fileId: FileInfoId, fileInfo: FileInfo, s: Stream[F, Byte]): F[Unit] =
    for {
      conf <- C.ask
      _ <-
        s.through(
          fs2.io.file.writeAll[F](new java.io.File(conf.basePath, fileId.toString).toPath, blk),
        ).compile
          .drain
    } yield ()

  def writeAll(fileInfo: FileInfo, ss: Seq[Stream[F, Byte]]): F[Unit] = ???
}
