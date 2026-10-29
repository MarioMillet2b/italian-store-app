package com.floromilano.app

import android.app.Application

class StoreApplication : Application() {
    val vault by lazy { SecureVault(this) }
}
