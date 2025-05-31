package com.billbuddy.shared.db.shared

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.billbuddy.shared.db.BillBuddyDatabase
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<BillBuddyDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = BillBuddyDatabaseImpl.Schema

internal fun KClass<BillBuddyDatabase>.newInstance(driver: SqlDriver): BillBuddyDatabase =
    BillBuddyDatabaseImpl(driver)

private class BillBuddyDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), BillBuddyDatabase {
  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS User (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    email TEXT NOT NULL UNIQUE,
          |    displayName TEXT
          |)
          """.trimMargin(), 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
