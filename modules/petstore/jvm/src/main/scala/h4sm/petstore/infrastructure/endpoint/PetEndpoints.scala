package h4sm.petstore
package infrastructure.endpoint

import cats.effect.Sync
import cats.syntax.all._
import domain._
import h4sm.auth._
import h4sm.auth.domain.tokens._
import h4sm.auth.domain.tokens.AsBaseToken.ops._
import h4sm.petstore.infrastructure.endpoint.codecs._
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec._
import tsec.authentication._

class PetEndpoints[F[_]: Sync: PetAlgebra, T[_]](auth: UserSecuredRequestHandler[F, T])(implicit
    B: AsBaseToken[T[UserId]],
) extends Http4sDsl[F] {
  def addPet =
    UserAuthService[F, T] { case req @ POST -> Root asAuthed _ =>
      for {
        pr <- req.request.as[PetRequest]
        _ <- PetAlgebra[F].insert(
          Pet(pr.name, pr.bio, req.authenticator.asBase.identity, pr.status),
        )
        res <- Ok()
      } yield res
    }

  def listPets =
    UserAuthService[F, T] { case GET -> Root asAuthed _ =>
      for {
        pets <- PetAlgebra[F].select
        res <- Ok(pets)
      } yield res
    }

  val authService = addPet <+> listPets

  val endpoints = auth.liftService(authService)
}
