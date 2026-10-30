package com.floromilano.app

import java.net.URI

object StoreUrl {
    fun normalize(value: String): String {
        val uri = URI(value.trim())
        require(uri.scheme == "https" && !uri.host.isNullOrBlank()) {
            "Usa un indirizzo HTTPS valido."
        }
        require(uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "L’indirizzo non deve contenere credenziali o parametri."
        }
        require(uri.path.isNullOrBlank() || uri.path == "/") {
            "Inserisci solo l’indirizzo del negozio, senza percorsi."
        }
        return uri.toASCIIString().trimEnd('/')
    }
}
