package dev.projectortv.tv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import dev.projectortv.catalog.BingeBangParser
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException

/** Small image cache and bounded thumbnails for older TV hardware. */
class PosterLoader(private val executor: ExecutorService, private val onUi: ((() -> Unit) -> Unit)) {
    @Volatile private var closed = false
    private val cache = object : LruCache<String, Bitmap>(12 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun into(imageView: ImageView, url: String?) {
        imageView.tag = url
        imageView.setImageDrawable(null)
        if (closed || url == null || BingeBangParser.posterUrlFromRaw(url) == null) return
        cache.get(url)?.let { imageView.setImageBitmap(it); return }
        try { executor.execute {
            val bitmap = try { download(url) } catch (_: Exception) { null }
            if (bitmap != null && !closed) {
                synchronized(cache) { cache.put(url, bitmap) }
                onUi {
                    if (!closed && imageView.tag == url) imageView.setImageBitmap(bitmap)
                }
            }
        } } catch (_: RejectedExecutionException) { /* Activity already disposed. */ }
    }

    fun close() { closed = true; cache.evictAll() }

    private fun download(url: String): Bitmap? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6500
            readTimeout = 6500
            instanceFollowRedirects = false
            setRequestProperty("Accept", "image/*")
        }
        return try {
            if (conn.responseCode != 200 || conn.contentLengthLong > 3_500_000) return null
            conn.inputStream.use { stream ->
                val buffer = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(16 * 1024)
                while (true) {
                    val amount = stream.read(chunk)
                    if (amount < 0) break
                    if (buffer.size() + amount > 3_500_000) return null
                    buffer.write(chunk, 0, amount)
                }
                val bytes = buffer.toByteArray()
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                val maxDimension = maxOf(options.outWidth, options.outHeight)
                if (maxDimension <= 0 || maxDimension > 12000) return null
                var sample = 1
                while (maxDimension / sample > 720) sample *= 2
                val scaled = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, scaled)
            }
        } finally { conn.disconnect() }
    }
}
