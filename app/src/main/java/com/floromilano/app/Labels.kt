package com.floromilano.app

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.provider.OpenableColumns
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object Labels {
    fun bitmap(
        title: String,
        detail: String,
        barcode: String,
        widthMm: Int,
        heightMm: Int,
    ): Bitmap {
        require(widthMm in 40..100 && heightMm in 25..100)
        val width = widthMm * 8 // Approximately 203 dpi.
        val height = heightMm * 8
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap).apply { drawColor(Color.WHITE) }
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textAlign = Paint.Align.CENTER
            }
        fun line(text: String, y: Float, size: Float) {
            paint.textSize = size
            var display = text
            while (display.isNotEmpty() && paint.measureText(display) > width - 24) display =
                display.dropLast(1)
            canvas.drawText(display, width / 2f, y, paint)
        }
        line(title, height * .14f, height * .09f)
        line(detail, height * .28f, height * .065f)
        val matrix =
            MultiFormatWriter()
                .encode(
                    barcode,
                    BarcodeFormat.CODE_128,
                    width - 24,
                    (height * .45).toInt(),
                    mapOf(EncodeHintType.MARGIN to 12),
                )
        paint.style = Paint.Style.FILL
        for (x in 0 until matrix.width) {
            if (matrix[x, 0])
                canvas.drawRect(
                    (x + 12).toFloat(),
                    height * .35f,
                    (x + 13).toFloat(),
                    height * .35f + matrix.height,
                    paint,
                )
        }
        line(barcode, height * .93f, height * .07f)
        return bitmap
    }

    fun share(context: Context, bitmap: Bitmap) {
        val directory = File(context.cacheDir, "labels").apply { mkdirs() }
        directory
            .listFiles()
            ?.filter { it.lastModified() < System.currentTimeMillis() - 86400000 }
            ?.forEach { it.delete() }
        val file = File(directory, UUID.randomUUID().toString() + ".png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = Uri.parse("content://${context.packageName}.labels/${file.name}")
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Condividi etichetta",
            )
        )
    }

    fun print(context: Context, bitmap: Bitmap, widthMm: Int, heightMm: Int) {
        val attributes =
            PrintAttributes.Builder()
                .setMediaSize(
                    PrintAttributes.MediaSize(
                        "floro-label",
                        "Etichetta",
                        (widthMm * 1000 / 25.4).toInt(),
                        (heightMm * 1000 / 25.4).toInt(),
                    )
                )
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()
        (context.getSystemService(Context.PRINT_SERVICE) as PrintManager).print(
            "Floro Milano · etichetta",
            object : PrintDocumentAdapter() {
                private var layout = attributes

                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes,
                    cancellationSignal: CancellationSignal,
                    callback: LayoutResultCallback,
                    extras: Bundle?,
                ) {
                    layout = newAttributes
                    if (cancellationSignal.isCanceled) callback.onLayoutCancelled()
                    else
                        callback.onLayoutFinished(
                            PrintDocumentInfo.Builder("etichetta.pdf")
                                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                .setPageCount(1)
                                .build(),
                            true,
                        )
                }

                override fun onWrite(
                    pages: Array<out PageRange>,
                    destination: ParcelFileDescriptor,
                    cancellationSignal: CancellationSignal,
                    callback: WriteResultCallback,
                ) {
                    if (cancellationSignal.isCanceled) {
                        callback.onWriteCancelled()
                        return
                    }
                    try {
                        val document = PrintedPdfDocument(context, layout)
                        try {
                            val page = document.startPage(0)
                            val area = page.info.contentRect
                            val scale =
                                minOf(
                                    area.width().toFloat() / bitmap.width,
                                    area.height().toFloat() / bitmap.height,
                                )
                            val w = (bitmap.width * scale).toInt()
                            val h = (bitmap.height * scale).toInt()
                            page.canvas.drawBitmap(
                                bitmap,
                                null,
                                Rect(area.left, area.top, area.left + w, area.top + h),
                                Paint(Paint.FILTER_BITMAP_FLAG),
                            )
                            document.finishPage(page)
                            FileOutputStream(destination.fileDescriptor).use {
                                document.writeTo(it)
                            }
                        } finally {
                            document.close()
                        }
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (_: Exception) {
                        callback.onWriteFailed("Impossibile creare il documento di stampa.")
                    }
                }
            },
            attributes,
        )
    }
}

/** Read-only temporary images; URI grants expose only the selected label. */
class LabelProvider : ContentProvider() {
    override fun onCreate() = true

    private fun file(uri: Uri): File {
        val directory = File(requireNotNull(context).cacheDir, "labels").canonicalFile
        val result = File(directory, uri.lastPathSegment ?: "").canonicalFile
        require(result.parentFile == directory && result.extension == "png" && result.isFile)
        return result
    }

    override fun getType(uri: Uri) = "image/png"

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        require(mode == "r")
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val file = file(uri)
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(columns).apply {
            addRow(columns.map { if (it == OpenableColumns.SIZE) file.length() else file.name })
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) =
        throw UnsupportedOperationException()

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = throw UnsupportedOperationException()
}
