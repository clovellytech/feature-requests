package h4sm
package store
package infrastructure.repository.sql

import cats.syntax.all._
import domain._
import doobie._
import doobie.implicits._
import h4sm.auth.db.sql._

trait ItemSql {
  type Annotated = (Item, ItemId, Instant)

  def insert(item: Item): Update0 = sql"""
    insert into ct_store.item (title, description, create_by, price)
    values (${item.title}, ${item.description}, ${item.createBy}, ${item.price})
  """.update

  def insertGetId(item: Item): ConnectionIO[ItemId] =
    insert(item).withUniqueGeneratedKeys("item_id")

  def select: Query0[Annotated] = sql"""
    select title, description, create_by, price, item_id, create_date
    from ct_store.item
  """.query

  def byId(itemId: ItemId): Query0[Annotated] = (select.toFragment ++ fr"""
    where item_id = $itemId
  """).query

  def delete(itemId: ItemId): Update0 = sql"""
    delete from ct_store.item
    where item_id = $itemId
  """.update

  def update(itemId: ItemId, item: Item): Update0 = sql"""
    update ct_store.item
    set title = ${item.title},
        description = ${item.description},
        create_by = ${item.createBy},
        price = ${item.price}
    where item_id = $itemId
  """.update

  def byIds(itemIds: List[ItemId]): Query0[Annotated] =
    (select.toFragment ++
      Fragments.whereAndOpt(itemIds.toNel.map(ids => Fragments.in(fr"item_id", ids)))).query
}
