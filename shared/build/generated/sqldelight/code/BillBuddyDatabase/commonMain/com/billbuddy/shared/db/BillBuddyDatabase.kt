package com.billbuddy.shared.db

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.billbuddy.shared.db.shared.newInstance
import com.billbuddy.shared.db.shared.schema
import kotlin.Unit

public interface BillBuddyDatabase : Transacter {
  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = BillBuddyDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): BillBuddyDatabase =
        BillBuddyDatabase::class.newInstance(driver)
  }
}
