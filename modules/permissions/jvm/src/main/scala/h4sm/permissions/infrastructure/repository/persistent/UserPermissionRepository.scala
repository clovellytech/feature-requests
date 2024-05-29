package h4sm.permissions.infrastructure.repository

import java.time.Instant

import cats.effect.Bracket
import cats.syntax.all._
import cats.data.OptionT
import doobie._
import doobie.implicits._
import h4sm.auth.UserId
import h4sm.permissions.domain._
import persistent.sql._

class UserPermissionRepository[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F])
    extends UserPermissionAlgebra[F] {
  def hasPermission(uid: UserId, appName: String, name: String): F[Boolean] =
    OptionT(userPermissions.userPermission(uid, appName, name).option).isDefined.transact(xa)

  def select: F[List[(UserPermission[PermissionId], UserPermissionId, Instant)]] =
    userPermissions.select.to[List].transact(xa)

  def byId(
      id: UserPermissionId,
  ): OptionT[F, (UserPermission[PermissionId], UserPermissionId, Instant)] =
    OptionT(userPermissions.byId(id).option.transact(xa))

  def insert(a: UserPermission[PermissionId]): F[Unit] =
    userPermissions.insert(a).run.as(()).transact(xa)

  def insertGetId(a: UserPermission[PermissionId]): OptionT[F, UserPermissionId] =
    OptionT.liftF(userPermissions.insertGetId(a).transact(xa))

  def delete(i: UserPermissionId): F[Unit] = userPermissions.delete(i).run.as(()).transact(xa)
}
