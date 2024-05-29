package h4sm.files
package infrastructure.endpoint

import java.io.File
import java.util.UUID

import cats.syntax.all._
import config._
import doobie.util.transactor.Transactor
import fs2.Stream
import org.http4s._
import org.http4s.dsl._
import org.http4s.headers.Location
import org.http4s.multipart._
import cats.effect._
import h4sm.auth._
import h4sm.auth.comm.SiteResult
import h4sm.auth.comm.codecs._
import h4sm.auth.domain.tokens.AsBaseToken.ops._
import h4sm.auth.domain.tokens.AsBaseToken
import h4sm.files.domain._
import h4sm.files.infrastructure.backends.{FileMetaService, LocalFileStoreService}
import tsec.authentication._

class FileEndpoints[F[_], T[_]](auth: UserSecuredRequestHandler[F, T])(implicit
    S: Sync[F],
    F: FileMetaAlgebra[F],
    FS: FileStoreAlgebra[F],
    CS: ContextShift[F],
    C: ConfigAsk[F],
    blk: Blocker,
    baseToken: AsBaseToken[T[UserId]],
) extends Http4sDsl[F] {
  val fileNotExists: F[File] =
    Error.fileNotExistError("Requested file not found").raiseError[F, File]

  def getFile(
      uuid: String,
      allowPred: FileInfo => Boolean,
  ): F[(FileInfo, FileInfoId, Stream[F, Byte])] =
    for {
      uuid <- S.delay(UUID.fromString(uuid))
      conf <- C.ask
      baseFile = new File(conf.basePath)
      fileInfo <- F.retrieveMeta(uuid)
      _ <- if (allowPred(fileInfo)) (new File(baseFile, uuid.toString)).pure[F] else fileNotExists
    } yield (fileInfo, uuid, FS.retrieve(uuid))

  def unAuthEndpoints: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "request" / uuidstr =>
        for {
          fileInfo <- getFile(uuidstr, _.isPublic)
          (meta, _, bytes) = fileInfo
          // need to figure out content type and filename parts.
          resp <- Ok(bytes)
        } yield resp

      case GET -> Root / "config" => C.ask.flatMap(c => Ok(c.uploadMax.toString))
    }

  def authFileEndpoints: UserAuthService[F, T] =
    UserAuthService { case req @ GET -> Root / uuidstr asAuthed _ =>
      val pred = (i: FileInfo) =>
        i.isPublic || (i.uploadedBy.compareTo(req.authenticator.asBase.identity) == 0)
      for {
        fileInfo <- getFile(uuidstr, pred)
        (_, _, bytes) = fileInfo
        resp <- Ok(bytes)
      } yield resp
    }

  def authInfoEndpoints: UserAuthService[F, T] = {
    // I import this here to avoid clashing with the raw bytes in authFileEndpoints
    // If this goes to the top of file, bytes in authFileEndpoints will have
    // an ambiguous implicit error.
    import org.http4s.circe.CirceEntityCodec._

    UserAuthService {
      case req @ GET -> Root asAuthed _ =>
        for {
          infos <- F.retrieveUserMeta(req.authenticator.asBase.identity)
          resp <- Ok(SiteResult(infos))
        } yield resp

      case req @ POST -> Root asAuthed _ =>
        val decoded = EntityDecoder[F, Multipart[F]].decode(req.request, true)
        decoded.value.flatMap(
          _.fold(
            df => {
              println("Decode failure")
              println(df)
              BadRequest()
            },
            mp =>
              for {
                savedFileIds <- mp.parts.traverse { part =>
                  val finfo = FileInfo(
                    part.name,
                    none,
                    part.filename,
                    none,
                    req.authenticator.asBase.identity,
                    false,
                  )
                  for {
                    finfoId <- F.storeMeta(finfo)
                    _ <- FS.write(finfoId, finfo, part.body)
                  } yield finfoId
                }
                resp <- Ok(SiteResult(savedFileIds.toList))
              } yield resp,
          ),
        )

      case POST -> Root / uuid asAuthed _ =>
        for {
          uuid <- S.delay(UUID.fromString(uuid))
          fileInfo <- F.retrieveMeta(uuid)
          url <-
            Uri
              .fromString(s"/$uuid/${fileInfo.filename.getOrElse("download")}")
              .leftWiden[Throwable]
              .liftTo[F]
          resp <- TemporaryRedirect(Location(url))
        } yield resp

      case req @ GET -> Root / uuid / _ asAuthed _ =>
        for {
          uuid <- S.delay(UUID.fromString(uuid))
          _ <- F.retrieveMeta(uuid)
          file <- FS.retrieveFile(uuid)
          resp <- StaticFile.fromFile[F](file, blk, req.request.some).getOrElseF(BadRequest())
        } yield resp
    }
  }

  def endpoints: HttpRoutes[F] =
    unAuthEndpoints <+> auth.liftService(authFileEndpoints <+> authInfoEndpoints)
}

object FileEndpoints {
  def persistingEndpoints[F[_]: Sync: ContextShift: ConfigAsk, T[_]](
      xa: Transactor[F],
      auth: UserSecuredRequestHandler[F, T],
      blk: Blocker,
  )(implicit b: AsBaseToken[T[UserId]]): FileEndpoints[F, T] = {
    implicit val bk = blk
    implicit val fileMeta = new FileMetaService[F](xa)
    implicit val fileStore = new LocalFileStoreService[F]()
    new FileEndpoints[F, T](auth)
  }
}
