package h4sm
package permissions
package infrastructure
package endpoint

import auth.infrastructure.endpoint._
import cats.effect.{IO, Sync}
import cats.implicits._
import doobie.Transactor
import auth.client.{AuthClient, IOTestAuthClientChecks, TestAuthClient}
import auth.domain.UserService
import auth.domain.tokens._
import auth.domain.users.UserRepositoryAlgebra
import auth.infrastructure.endpoint.arbitraries._
import auth.infrastructure.repository.persistent.{TokenRepositoryInterpreter, UserRepositoryInterpreter}
import testutil.DbFixtureSuite
import permissions.infrastructure.repository.{PermissionRepository, UserPermissionRepository}
import permissions.domain._
import org.scalatest._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tsec.passwordhashers.jca.BCrypt
import repository.persistent.sql.arbitraries._
import tsec.authentication.TSecBearerToken

class PermissionEndpointsTestSpec
extends Matchers
with ScalaCheckPropertyChecks
with DbFixtureSuite
with IOTestAuthClientChecks {

  def schemaNames: Seq[String] = Seq("ct_auth", "ct_permissions")

  case class Clients[F[_], Alg, T[_]](
    userRepo : UserRepositoryAlgebra[F],
    permRepo : PermissionAlgebra[F],
    userPermRepo : UserPermissionAlgebra[F],
    testAuthClient : TestAuthClient[F, Alg, T],
    permClient : PermissionClient[F, Alg, TSecBearerToken])

  def clients(xa : Transactor[IO]) : Clients[IO, BCrypt, TSecBearerToken] = {
    implicit val userRepo = new UserRepositoryInterpreter(xa)
    val userService = new UserService[IO, BCrypt](BCrypt)
    implicit val tokenService = new TokenRepositoryInterpreter(xa)
    val authEndpoints = new AuthEndpoints(userService, Authenticators.bearer[IO])
    implicit val permRepo = new PermissionRepository[IO](xa)
    implicit val userPermRepo = new UserPermissionRepository[IO](xa)
    val permissionEndpoints = new PermissionEndpoints[IO, BCrypt, TSecBearerToken](authEndpoints)
    val authenticator = Authenticators.bearer[IO]
    val authClient = new AuthClient(userService, authenticator)
    val permissionClient = new PermissionClient(permissionEndpoints)
    val testAuthClient = new TestAuthClient(authClient)
    Clients(userRepo, permRepo, userPermRepo, testAuthClient, permissionClient)
  }

  def permitUser[F[_]: Sync, Alg, T[_]](cs : Clients[F, Alg, T])(
    ur : UserRequest,
    p : Permission
  ) : F[PermissionId] = for {
    userDetails <- cs.userRepo.byUsername(ur.username).getOrElse(fail)
    permId <- cs.permRepo.selectByAttributes(p.appName, p.name).map(_._2).orElse(cs.permRepo.insertGetId(p)).getOrElse(fail)
    _ <- cs.userPermRepo.insert(UserPermission(userDetails._2, permId, userDetails._2))
  } yield permId

  test("a logged in non admin user should not be able to add a permission") { p =>
    val cs = clients(p.transactor)

    forAnyUser2(cs.testAuthClient) { implicit h => (_: UserRequest, p: Permission) =>
      cs.permClient.addPermission(p).attempt.map(_.isLeft should equal(true))
    }
  }

  test("a logged in permissioned user be able to add a permission") { p =>
    val cs = clients(p.transactor)

    forAnyUser2(cs.testAuthClient) { implicit h => (ur: UserRequest, p: Permission) =>
      val program = for {
        pid <- permitUser(cs)(ur, Permission("admin", "", "ct_permissions"))
        _ <- cs.permClient.addPermission(p)
        _ <- cs.permRepo.delete(pid)
      } yield ()

      program.attempt.map(_.isRight should equal(true))
    }
  }

}