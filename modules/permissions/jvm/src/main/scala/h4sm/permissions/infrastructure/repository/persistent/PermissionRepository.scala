package h4sm.permissions.infrastructure.repository

import cats.effect.Bracket
import cats.syntax.all._
import cats.data.OptionT
import doobie._
import doobie.implicits._
import h4sm.permissions.domain.{Permission, PermissionAlgebra, PermissionId}
import persistent.sql.permissions

class PermissionRepository[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F])
    extends PermissionAlgebra[F] {
  def insert(a: Permission): F[Unit] = permissions.insert(a).run.as(()).transact(xa)

  def insertGetId(a: Permission): OptionT[F, PermissionId] =
    OptionT.liftF(permissions.insertGetId(a).transact(xa))

  def delete(i: PermissionId): F[Unit] = permissions.delete(i).run.as(()).transact(xa)

  def select: F[List[(Permission, PermissionId, Unit)]] =
    permissions.select.to[List].map(_.map { case (p, pid) => (p, pid, ()) }).transact(xa)

  def byId(id: PermissionId): OptionT[F, (Permission, PermissionId, Unit)] =
    OptionT(permissions.byId(id).option.transact(xa)).map((_, id, ()))

  def update(id: PermissionId, u: Permission): F[Unit] =
    permissions.update(id, u).run.as(()).transact(xa)

  def updateUnique(u: Permission): F[Unit] = permissions.updateUnique(u).run.void.transact(xa)

  def selectByAppName(appName: String): F[List[(Permission, PermissionId)]] =
    permissions.byAppName(appName).to[List].transact(xa)

  def selectByAttributes(appName: String, name: String): OptionT[F, (Permission, PermissionId)] =
    OptionT(permissions.byAttributes(appName: String, name: String).option.transact(xa))
}
