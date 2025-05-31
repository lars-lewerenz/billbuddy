package com.billbuddy.shared

import platform.UIKit.UIDevice

actual interface Platform {
    actual val name: String
}

actual fun getPlatform(): Platform = object : Platform {
    actual val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}
