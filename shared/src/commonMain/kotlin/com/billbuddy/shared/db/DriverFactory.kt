package com.billbuddy.shared.db

import app.cash.sqldelight.db.SqlDriver

// Common expect declaration for creating a SQLDelight driver
expect fun createDriver(): SqlDriver
