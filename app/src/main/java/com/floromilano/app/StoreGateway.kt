package com.floromilano.app

import org.json.JSONObject

interface StoreGateway {
    suspend fun request(
        path: String,
        method: String = "GET",
        payload: JSONObject? = null,
    ): JSONObject
}
