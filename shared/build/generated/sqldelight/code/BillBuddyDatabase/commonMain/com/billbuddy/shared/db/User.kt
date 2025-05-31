package com.billbuddy.shared.db

import kotlin.String

public data class User(
  public val id: String,
  public val email: String,
  public val displayName: String?,
)
