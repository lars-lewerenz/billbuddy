package com.billbuddy.shared

actual interface Platform {
    actual val name: String
}

actual fun getPlatform(): Platform = object : Platform {
    actual val name: String = "Android"
}
