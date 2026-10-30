package com.floromilano.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun money(cents: Long, currency: String): String =
    NumberFormat.getCurrencyInstance(Locale.getDefault())
        .apply { this.currency = Currency.getInstance(currency) }
        .format(BigDecimal.valueOf(cents, 2))

/** Public catalogue thumbnails only. Never attach the device bearer token. */
internal object ProductImages {
    val cache =
        object : LruCache<String, Bitmap>(6 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap) = value.byteCount
        }

    suspend fun load(value: String): Bitmap? =
        withContext(Dispatchers.IO) {
            cache.get(value)?.let {
                return@withContext it
            }
            if (runCatching { URI(value).scheme }.getOrNull() != "https") return@withContext null
            val connection = URL(value).openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                if (connection.responseCode != 200) return@withContext null
                val bytes =
                    connection.inputStream.use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (output.size() <= 4 * 1024 * 1024) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                if (bytes.size > 4 * 1024 * 1024) return@withContext null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 640) sample *=
                    2
                BitmapFactory.decodeByteArray(
                        bytes,
                        0,
                        bytes.size,
                        BitmapFactory.Options().apply { inSampleSize = sample },
                    )
                    ?.also { cache.put(value, it) }
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }
}

@Composable
internal fun ProductPhoto(url: String?, modifier: Modifier = Modifier) {
    val bitmap by
        produceState<Bitmap?>(url?.let { ProductImages.cache.get(it) }, url) {
            value = url?.let { ProductImages.load(it) }
        }
    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let {
            Image(
                it.asImageBitmap(),
                null,
                Modifier.fillMaxSize().padding(4.dp),
                contentScale = ContentScale.Fit,
            )
        }
            ?: Icon(
                painterResource(R.drawable.ic_sell),
                null,
                Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
    }
}

@Composable
internal fun QuantityControl(
    quantity: Int,
    minus: () -> Unit,
    plus: () -> Unit,
    enabled: Boolean = true,
    canPlus: Boolean = true,
    canMinus: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = minus, enabled = enabled && canMinus) {
            Icon(
                painterResource(R.drawable.ic_minus),
                stringResource(R.string.decrease_quantity),
                Modifier.size(18.dp),
            )
        }
        Text(
            quantity.toString(),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.widthIn(min = 18.dp),
        )
        IconButton(onClick = plus, enabled = enabled && canPlus) {
            Icon(
                painterResource(R.drawable.ic_plus),
                stringResource(R.string.increase_quantity),
                Modifier.size(18.dp),
            )
        }
    }
}

@Composable
internal fun Choice(
    label: String,
    selected: Int?,
    options: List<NamedItem>,
    empty: String,
    onSelect: (Int?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("$label: ${options.find { it.id == selected }?.name ?: empty}")
        }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(
                text = { Text(empty) },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name) },
                    onClick = {
                        expanded = false
                        onSelect(item.id)
                    },
                )
            }
        }
    }
}
