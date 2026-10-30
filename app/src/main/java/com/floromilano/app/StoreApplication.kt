package com.floromilano.app

import android.app.Application

class StoreApplication : Application() {
    val vault by lazy { SecureVault(this) }
    // Dependency seam for the separate instrumentation APK; never set by
    // application settings, intents or production startup.
    internal var gatewayOverride: StoreGateway? = null
}
