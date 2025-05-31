package com.mykmmproject.shared

expect class Platform() {
    val name: String
}

expect fun getPlatform(): Platform
