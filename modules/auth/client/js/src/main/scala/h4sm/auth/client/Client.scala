package h4sm.auth
package client

import cats.syntax.all._
import cats.MonadError
import cats.data.StateT
import h4sm.auth.comm.codecs._
import h4sm.common.API
import h4sm.auth.comm.{SiteResult, UserDetail, UserRequest}

class Client[F[_]: MonadError[?[_], Throwable]](implicit val F: API[F]) {
  def base = ""
  def loginRoute = s"$base/login"
  def signupRoute = s"$base/user"
  def profileRoute = s"$base/user"
  def logoutRoute = s"$base/logout"
  def testRoute = s"$base/istest"
  def deleteRoute(username: String) = s"$base/$username"

  def isTest: F[Boolean] = F.get(testRoute).flatMap(_.as[Boolean]).recover { case _ => false }

  type Session[A] = StateT[F, F.H, A]

  def getSession(ur: UserRequest): Session[String] =
    for {
      resp <- F.postT(loginRoute, ur)
      siteRes <- StateT.liftF(resp.as[SiteResult[String]])
    } yield siteRes.result

  def currentUser: Session[UserDetail] =
    for {
      resp <- F.getT(profileRoute)
      user <- StateT.liftF(resp.as[SiteResult[UserDetail]])
    } yield user.result

  def delete(username: String): F[Unit] = F.delete(deleteRoute(username)).void

  def signup(ur: UserRequest): F[Unit] = F.post(signupRoute, ur).void

  def logout: Session[Unit] = F.postT(logoutRoute, ()).void
}

object Client {
  def apply[F[_]: MonadError[?[_], Throwable]: API](baseUrl: String): Client[F] =
    new Client[F] {
      override def base = baseUrl
    }
}
