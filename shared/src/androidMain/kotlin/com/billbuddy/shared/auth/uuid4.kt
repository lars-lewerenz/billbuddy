package com.billbuddy.shared.auth
import java.util.UUID
actual fun uuid4(): String = UUID.randomUUID().toString()
