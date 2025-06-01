package com.billbuddy.shared.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// KoinComponent to inject context, needed by AndroidSqliteDriver
internal object DriverFactoryContext : KoinComponent {
    val context: Context by inject()
}

actual fun createDriver(): SqlDriver {
    return AndroidSqliteDriver(BillBuddyDatabase.Schema, DriverFactoryContext.context, "BillBuddyDatabase.db")
}
