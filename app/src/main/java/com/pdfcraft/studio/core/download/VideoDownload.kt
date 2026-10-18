package com.pdfcraft.studio.core.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

sealed class UrlCheck {
    class Social(val url: String, val host: String) : UrlCheck()
    class Direct(val url: String) : UrlCheck()
    class MaybeFile(val url: String) : UrlCheck()
    class Bad(val message: String) : UrlCheck()
}

interface VideoFormatProvider {
    fun lookup(context: Context, check: UrlCheck): ProviderResult
}

sealed class ProviderResult {
    class Formats(val url: String, val formats: List<VideoFormat>) : ProviderResult()
    class Unavailable(val url: String, val message: String) : ProviderResult()
    class Failed(val message: String) : ProviderResult()
}

class VideoFormat(
    val label: String,
    val mime: String,
    val bytes: Long,
    val downloadUrl: String
)

object VideoUrls {
    private val youtubeHosts = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtube-nocookie.com",
        "www.youtube-nocookie.com",
        "youtu.be",
        "www.youtu.be"
    )
    private val facebookHosts = setOf(
        "facebook.com",
        "www.facebook.com",
        "m.facebook.com",
        "mbasic.facebook.com",
        "web.facebook.com",
        "fb.watch",
        "www.fb.watch",
        "fb.com",
        "www.fb.com"
    )
    private val tiktokHosts = setOf(
        "tiktok.com",
        "www.tiktok.com",
        "m.tiktok.com",
        "vm.tiktok.com",
        "vt.tiktok.com"
    )
    private val videoExt = setOf("mp4", "webm", "mkv", "mov", "m4v", "3gp")

    fun validate(raw: String): UrlCheck {
        val text = raw.trim()
        if (text.isEmpty()) return UrlCheck.Bad("Enter a video URL.")
        val lower = text.lowercase(Locale.US)
        if (lower.startsWith("http://")) return UrlCheck.Bad("Only HTTPS links are allowed.")
        if (lower.startsWith("javascript:") || lower.startsWith("file:") || lower.startsWith("content:")) {
            return UrlCheck.Bad("This link type is not allowed.")
        }
        val start = lower.indexOf("https://")
        if (start < 0) return UrlCheck.Bad("Enter a valid HTTPS link.")
        var end = text.length
        val stops = charArrayOf(' ', '\n', '\r', '\t')
        var i = start
        while (i < text.length) {
            if (stops.contains(text[i])) {
                end = i
                break
            }
            i++
        }
        val slice = text.substring(start, end).trim()
        val uri = try {
            Uri.parse(slice)
        } catch (_: Throwable) {
            return UrlCheck.Bad("This URL is not valid.")
        }
        val scheme = uri.scheme?.lowercase(Locale.US) ?: return UrlCheck.Bad("This URL is not valid.")
        if (scheme != "https") return UrlCheck.Bad("Only HTTPS links are allowed.")
        var host = uri.host?.lowercase(Locale.US)?.trim('.') ?: return UrlCheck.Bad("This URL has no host.")
        if (host.isEmpty() || host.contains(" ") || host.contains("@") || host.contains("\\")) {
            return UrlCheck.Bad("This URL is not valid.")
        }
        if (host == "localhost" || host == "127.0.0.1" || host == "0.0.0.0" || host == "::1") {
            return UrlCheck.Bad("This host is not allowed.")
        }
        if (isPrivateHost(host)) {
            return UrlCheck.Bad("This host is not allowed.")
        }
        val normalized = uri.toString()
        if (youtubeHosts.contains(host)) return checkYoutube(uri, normalized, host)
        if (facebookHosts.contains(host)) return checkFacebook(uri, normalized, host)
        if (tiktokHosts.contains(host)) return checkTikTok(uri, normalized, host)
        if (extensionOf(uri) in videoExt) return UrlCheck.Direct(normalized)
        return UrlCheck.MaybeFile(normalized)
    }

    private fun isPrivateHost(h: String): Boolean {
        if (h.endsWith(".local") || h.endsWith(".internal") || h.endsWith(".localhost")) return true
        if (h.startsWith("[") || h.contains(":")) return true
        val p = h.split('.')
        if (p.size == 4 && p.all { it.isNotEmpty() && it.length <= 3 && it.all { c -> c.isDigit() } }) {
            val a = p[0].toInt()
            val b = p[1].toInt()
            return a == 10 || a == 127 || a == 0 ||
                (a == 169 && b == 254) ||
                (a == 172 && b in 16..31) ||
                (a == 192 && b == 168)
        }
        return false
    }

    private fun checkYoutube(uri: Uri, url: String, host: String): UrlCheck {
        val path = uri.path.orEmpty()
        if (host == "youtu.be" || host == "www.youtu.be") {
            val id = path.trim('/').substringBefore('/')
            return if (id.length >= 6) UrlCheck.Social(url, host) else UrlCheck.Bad("This YouTube link has no video id.")
        }
        val parts = path.split('/').filter { it.isNotEmpty() }
        if (parts.isNotEmpty()) {
            val head = parts[0].lowercase(Locale.US)
            if (head == "shorts" || head == "embed" || head == "live" || head == "v") {
                val id = if (parts.size > 1) parts[1] else ""
                return if (id.length >= 6) UrlCheck.Social(url, host) else UrlCheck.Bad("This YouTube link has no video id.")
            }
        }
        if (path == "/watch" || path.startsWith("/watch")) {
            val id = uri.getQueryParameter("v").orEmpty()
            return if (id.length >= 6) UrlCheck.Social(url, host) else UrlCheck.Bad("This YouTube link has no video id.")
        }
        return UrlCheck.Bad("This YouTube link is not a video page.")
    }

    private fun checkFacebook(uri: Uri, url: String, host: String): UrlCheck {
        if (host == "fb.watch" || host == "www.fb.watch") {
            val id = uri.path.orEmpty().trim('/')
            return if (id.length >= 3) UrlCheck.Social(url, host) else UrlCheck.Bad("This Facebook link is not a video.")
        }
        val path = uri.path.orEmpty().lowercase(Locale.US)
        val query = uri.query.orEmpty().lowercase(Locale.US)
        val ok = path.contains("/videos/") ||
            path.contains("/reel/") ||
            path.contains("/reels/") ||
            path.contains("/watch") ||
            path.contains("/share/") ||
            query.contains("v=") ||
            query.contains("story_fbid")
        return if (ok) UrlCheck.Social(url, host) else UrlCheck.Bad("This Facebook link is not a video.")
    }

    private fun checkTikTok(uri: Uri, url: String, host: String): UrlCheck {
        val path = uri.path.orEmpty()
        if (host == "vm.tiktok.com" || host == "vt.tiktok.com") {
            return if (path.trim('/').length >= 3) UrlCheck.Social(url, host) else UrlCheck.Bad("This TikTok link is not a video.")
        }
        val lower = path.lowercase(Locale.US)
        val ok = lower.contains("/video/") || lower.startsWith("/t/")
        return if (ok) UrlCheck.Social(url, host) else UrlCheck.Bad("This TikTok link is not a video.")
    }

    private fun extensionOf(uri: Uri): String {
        val last = uri.lastPathSegment.orEmpty().lowercase(Locale.US)
        val dot = last.lastIndexOf('.')
        if (dot < 0 || dot == last.length - 1) return ""
        return last.substring(dot + 1)
    }
}

object CompliantVideoProvider : VideoFormatProvider {
    override fun lookup(context: Context, check: UrlCheck): ProviderResult {
        return when (check) {
            is UrlCheck.Bad -> ProviderResult.Failed(check.message)
            is UrlCheck.Social -> ProviderResult.Unavailable(
                check.url,
                "This is a valid " + check.host + " link. No authorized provider returned a video file, so nothing was downloaded. Checking the link does not mean the video can be saved."
            )
            is UrlCheck.Direct -> probe(check.url)
            is UrlCheck.MaybeFile -> probe(check.url)
        }
    }

    private fun probe(url: String): ProviderResult {
        val head = request(url, "HEAD")
        val info = if (head == null || head.code == 405 || head.code == 501 || head.code == 400) {
            request(url, "GET")
        } else head
        if (info == null) return ProviderResult.Failed("Could not reach this link. Check your connection and try again.")
        if (info.scheme != "https") return ProviderResult.Failed("The link redirected away from HTTPS.")
        if (info.code >= 400) return ProviderResult.Failed("The server rejected this link (" + info.code + ").")
        val type = info.mime.lowercase(Locale.US).substringBefore(';').trim()
        val video = type.startsWith("video/") ||
            ((type == "application/octet-stream" || type.isEmpty()) && info.ext in setOf("mp4", "webm", "mkv", "mov", "m4v", "3gp"))
        if (!video) {
            return ProviderResult.Failed("This link is not a direct video file, and it is not a supported YouTube, Facebook, or TikTok video URL.")
        }
        val label = if (info.ext.isEmpty()) "Original video" else "Original ." + info.ext
        return ProviderResult.Formats(
            info.finalUrl,
            listOf(VideoFormat(label, if (type.isEmpty()) "video/mp4" else type, info.bytes, info.finalUrl))
        )
    }

    private class Probe(val code: Int, val mime: String, val bytes: Long, val finalUrl: String, val scheme: String, val ext: String)

    private fun request(url: String, method: String): Probe? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection)
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            conn.requestMethod = method
            conn.setRequestProperty("User-Agent", "PDFCraftStudio")
            if (method == "GET") conn.setRequestProperty("Range", "bytes=0-0")
            val code = conn.responseCode
            val mime = conn.contentType ?: ""
            val bytes = conn.contentLengthLong
            val finalUri = Uri.parse(conn.url.toString())
            val scheme = finalUri.scheme?.lowercase(Locale.US) ?: ""
            val last = finalUri.lastPathSegment.orEmpty().lowercase(Locale.US)
            val dot = last.lastIndexOf('.')
            val ext = if (dot >= 0 && dot < last.length - 1) last.substring(dot + 1) else ""
            Probe(code, mime, bytes, finalUri.toString(), scheme, ext)
        } catch (_: Throwable) {
            null
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }
}

object VideoSaver {
    fun enqueue(context: Context, format: VideoFormat): Long {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val name = safeName(format.downloadUrl, format.mime)
        val req = DownloadManager.Request(Uri.parse(format.downloadUrl))
        req.setTitle(name)
        req.setDescription("PDFCraft Studio")
        req.setMimeType(format.mime)
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        req.setAllowedOverMetered(true)
        req.setAllowedOverRoaming(true)
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "PDFCraftStudio/" + name)
        return dm.enqueue(req)
    }

    fun remove(context: Context, id: Long) {
        if (id < 0L) return
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.remove(id)
        } catch (_: Throwable) {}
    }

    class Progress(val status: Int, val done: Long, val total: Long, val reason: Int)

    fun query(context: Context, id: Long): Progress? {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = dm.query(DownloadManager.Query().setFilterById(id)) ?: return null
        return cursor.use {
            if (!it.moveToFirst()) {
                null
            } else {
                val st = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val done = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val reasonIdx = it.getColumnIndex(DownloadManager.COLUMN_REASON)
                val reason = if (reasonIdx >= 0) it.getInt(reasonIdx) else 0
                Progress(st, done, total, reason)
            }
        }
    }

    private fun safeName(url: String, mime: String): String {
        val uri = Uri.parse(url)
        var name = uri.lastPathSegment.orEmpty()
        val q = name.indexOf('?')
        if (q >= 0) name = name.substring(0, q)
        name = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        if (name.length > 80) name = name.substring(name.length - 80)
        if (name.isBlank() || name == "_" || name == ".") name = "video"
        val lower = name.lowercase(Locale.US)
        val hasExt = lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mkv") ||
            lower.endsWith(".mov") || lower.endsWith(".m4v") || lower.endsWith(".3gp")
        if (!hasExt) {
            val ext = when {
                mime.contains("webm") -> ".webm"
                mime.contains("matroska") || mime.contains("mkv") -> ".mkv"
                mime.contains("quicktime") -> ".mov"
                mime.contains("3gpp") -> ".3gp"
                else -> ".mp4"
            }
            name = name + ext
        }
        return name
    }
}
