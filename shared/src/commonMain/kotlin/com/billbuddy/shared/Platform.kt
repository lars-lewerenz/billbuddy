package com.billbuddy.shared

expect interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
