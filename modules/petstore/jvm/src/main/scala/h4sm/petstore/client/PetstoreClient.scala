package h4sm.petstore
package client

import cats.effect.Sync
import cats.syntax.all._
import org.http4s.Headers
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.circe.CirceEntityCodec._
import h4sm.testutil.infrastructure.endpoints._
import h4sm.petstore.infrastructure.endpoint._
import h4sm.petstore.infrastructure.endpoint.codecs._
import org.http4s.Uri

class PetstoreClient[F[_]: Sync, T[_]](ps: PetEndpoints[F, T], os: OrderEndpoints[F, T])
    extends Http4sDsl[F]
    with Http4sClientDsl[F]
    with SessionClientDsl[F] {
  val pets = ps.endpoints.orNotFound
  val orders = os.endpoints.orNotFound

  def addPet(p: PetRequest)(implicit h: Headers): F[Unit] =
    for {
      req <- post(p, Uri.uri("/"))
      resp <- pets.run(req)
      _ <- passOk(resp)
    } yield ()

  def orderPet(order: OrderRequest)(implicit h: Headers): F[Unit] =
    for {
      req <- post(order, Uri.uri("/"))
      resp <- orders.run(req)
      _ <- passOk(resp)
    } yield ()
}
