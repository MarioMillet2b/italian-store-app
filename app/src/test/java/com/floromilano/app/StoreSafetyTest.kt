package com.floromilano.app

import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class StoreSafetyTest {
    @Test
    fun storeAddressRequiresAnHttpsOriginWithoutCredentialsOrQuery() {
        assertEquals(
            "https://staging.example.com",
            StoreUrl.normalize(" https://staging.example.com/ "),
        )
        for (url in
            listOf(
                "http://example.com",
                "https://user:secret@example.com",
                "https://example.com/?token=secret",
                "https://example.com/store",
                "https://example.com/#token",
            )) {
            assertTrue(url, runCatching { StoreUrl.normalize(url) }.isFailure)
        }
    }

    @Test
    fun moneyInputNeverRoundsOrUsesBinaryFloatingPoint() {
        assertEquals(9500L, priceCents("95,00"))
        assertEquals(1L, priceCents("0.01"))
        assertEquals(0L, priceCents("0"))
        for (value in
            listOf("1.001", "-1", "NaN", "", "1.000.000,00", "99999999999999")) assertNull(
            value,
            priceCents(value),
        )
    }

    @Test
    fun apiNeverFollowsRedirectsWithTheDeviceToken() = runBlocking {
        val redirected = AtomicBoolean(false)
        TestHttpServer { path ->
                if (path == "/capture") redirected.set(true)
                "HTTP/1.1 302 Found\r\nLocation: /capture\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
            }
            .use { server ->
                val result = runCatching {
                    StoreApi(server.url, "test-only-token").request("/context")
                }
                assertEquals(302, (result.exceptionOrNull() as ApiFailure).status)
                assertFalse(redirected.get())
            }
    }

    @Test
    fun apiShowsValidationMessagesWithoutMistakingThemForSuccess() = runBlocking {
        TestHttpServer {
                val body =
                    """{"message":"Validation failed","errors":{"quantity":["Quantità insufficiente"]}}"""
                "HTTP/1.1 422 Unprocessable Entity\r\nContent-Type: application/json\r\nContent-Length: ${body.toByteArray().size}\r\nConnection: close\r\n\r\n$body"
            }
            .use { server ->
                val result = runCatching {
                    StoreApi(server.url, "test-only-token").request("/context")
                }
                assertEquals(422, (result.exceptionOrNull() as ApiFailure).status)
                assertEquals("Quantità insufficiente", result.exceptionOrNull()?.message)
            }
    }
}

/** A real loopback HTTP responder, confined to tests. */
private class TestHttpServer(private val reply: (String) -> String) : Closeable {
    private val socket = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
    val url = "http://127.0.0.1:${socket.localPort}"
    private val worker =
        thread(isDaemon = true) {
            try {
                while (!socket.isClosed) socket.accept().use { client ->
                    val reader = client.getInputStream().bufferedReader()
                    val path = reader.readLine().split(' ')[1]
                    while (!reader.readLine().isNullOrBlank()) {
                        /* Consume request headers. */
                    }
                    client.getOutputStream().write(reply(path).toByteArray(Charsets.UTF_8))
                }
            } catch (_: java.net.SocketException) {
                /* Closed by the test. */
            }
        }

    override fun close() {
        socket.close()
        worker.join(1000)
    }
}
