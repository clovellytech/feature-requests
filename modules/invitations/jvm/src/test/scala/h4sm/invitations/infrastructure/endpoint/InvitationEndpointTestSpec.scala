package h4sm
package invitations.infrastructure.endpoint

import auth.client.IOTestAuthClientChecks
import auth.comm.arbitraries._
import auth.comm.UserRequest
import cats.effect.{IO, Sync}
import cats.syntax.all._
import testutil.EndpointTestSpec
import doobie.Transactor
import invitations.client.InvitationClientRunner
import org.http4s.Headers
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class InvitationEndpointTestSpec
    extends EndpointTestSpec
    with IOTestAuthClientChecks
    with ScalaCheckPropertyChecks
    with Matchers {
  def schemaNames: Seq[String] = List("ct_auth", "ct_invitations")

  test("Existing user can invite a new user") { p =>
    new InvitationClientRunner[IO] {
      val xa: Transactor[IO] = p.transactor
      forAnyUser2(testAuthClient) {
        implicit h: Headers => (_: UserRequest, ps: (String, UserRequest)) =>
          val (toName, newU) = ps
          createInvite(toName, newU).map { case (invite, _, _) =>
            invite.toEmail should equal(newU.username)
          }
      }
    }
  }

  test("Invited user should be able to open an invitation") { p =>
    new InvitationClientRunner[IO] {
      val xa: Transactor[IO] = p.transactor
      forAnyUser2(testAuthClient) {
        implicit h: Headers => (_: UserRequest, ps: (String, UserRequest)) =>
          val (toName, newU) = ps
          val job = for {
            (invite, _, _) <- createInvite(toName, newU)
            _ <- invitationClient.openInvite(InvitationByCodeRequest(newU.username, invite.code))
            (savedInvite, _, _) <-
              invitationAlg
                .fromToEmail(newU.username)
                .getOrElseF(Sync[IO].raiseError(new Exception("invite not found")))
          } yield savedInvite.openDate shouldBe defined

          job.recoverWith { case e: Throwable =>
            fail(e)
          }
      }
    }
  }
}
