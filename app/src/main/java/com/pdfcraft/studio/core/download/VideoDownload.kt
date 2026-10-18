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
    private val instagramHosts = setOf(
        "instagram.com",
        "www.instagram.com",
        "m.instagram.com"
    )
    private val videoExt = setOf("mp4", "webm", "mkv", "mov", "m4v", "3gp")
    private val audioExt = setOf("mp3", "m4a", "aac", "wav", "ogg", "opus")

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
        if (instagramHosts.contains(host)) return checkInstagram(uri, normalized, host)
        val ext = extensionOf(uri)
        if (ext in videoExt || ext in audioExt) return UrlCheck.Direct(normalized)
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

    private fun checkInstagram(uri: Uri, url: String, host: String): UrlCheck {
        val path = uri.path.orEmpty().lowercase(Locale.US)
        val ok = path.contains("/reel/") || path.contains("/reels/") || path.contains("/p/") || path.contains("/tv/")
        return if (ok) UrlCheck.Social(url, host) else UrlCheck.Bad("This Instagram link is not a video page.")
    }

    private fun extensionOf(uri: Uri): String {
        val last = uri.lastPathSegment.orEmpty().lowercase(Locale.US)
        val dot = last.lastIndexOf('.')
        if (dot < 0 || dot == last.length - 1) return ""
        return last.substring(dot + 1)
    }
}

private object SocialVideoExtractor {
    private val endpoints = listOf(
        "https://www.y2mate.com/mates/analyze/ajax",
        "https://www.y2mate.com/mates/analyzeV2/ajax",
        "https://www.youtubepp.com/api/convert"
    )

    fun lookup(videoUrl: String): ProviderResult {
        for (endpoint in endpoints) {
            val body = fetch(endpoint, videoUrl) ?: continue
            val links = parse(body, videoUrl)
            if (links.isNotEmpty()) {
                return ProviderResult.Formats(videoUrl, links)
            }
        }
        return ProviderResult.Unavailable(
            videoUrl,
            "This social video could not be analyzed. Check the link, connection, or try again."
        )
    }

    private fun fetch(endpoint: String, videoUrl: String): String? {
        val url = if (endpoint.contains("?")) {
            endpoint + "&url=" + java.net.URLEncoder.encode(videoUrl, "UTF-8")
        } else {
            endpoint + "?url=" + java.net.URLEncoder.encode(videoUrl, "UTF-8")
        }
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection)
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "PDFCraftStudio")
            conn.setRequestProperty("Accept", "application/json,text/html,application/xhtml+xml,*/*")
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }

    private fun parse(body: String, fallback: String): List<VideoFormat> {
        val candidates = linkedMapOf<String, String>()
        val regex = Regex(
            """https?://[^\s"'<>\]]+(?:\.(?:mp4|m4a|mp3|aac|wav|ogg|opus|webm|m4v|3gp|mkv|mov))[^\s"'<>\]]*""",
            setOf(RegexOption.IGNORE_CASE)
        )
        for (match in regex.findAll(body)) {
            val raw = match.value.trim().trimEnd('"', '\'', ',', '>', ']')
            if (raw.isBlank()) continue
            val lower = raw.lowercase(Locale.US)
            if (!lower.startsWith("http://") && !lower.startsWith("https://")) continue
            val name = labelFrom(raw)
            if (name.isNotBlank()) {
                candidates[raw] = name
            }
        }
        if (candidates.isEmpty()) {
            val fallbackUrl = fallback.trim()
            if (fallbackUrl.startsWith("https://") && mediaCandidate(fallbackUrl)) {
                candidates[fallbackUrl] = labelFrom(fallbackUrl)
            }
        }
        val list = ArrayList<VideoFormat>()
        for ((url, label) in candidates.entries) {
            val mime = mimeOf(url)
            list.add(VideoFormat(label, mime, 0L, url))
        }
        return list.distinctBy { it.downloadUrl }.take(8)
    }

    private fun mediaCandidate(url: String): Boolean {
        val lower = url.lowercase(Locale.US)
        return lower.contains(".mp4") || lower.contains(".mp3") || lower.contains(".m4a") ||
            lower.contains(".aac") || lower.contains(".wav") || lower.contains(".ogg") ||
            lower.contains(".webm") || lower.contains(".m4v") || lower.contains(".3gp") ||
            lower.contains(".mkv") || lower.contains(".mov") || lower.contains(".opus")
    }

    private fun mimeOf(url: String): String {
        val lower = url.lowercase(Locale.US)
        return when {
            lower.contains(".webm") -> "video/webm"
            lower.contains(".mkv") -> "video/x-matroska"
            lower.contains(".mov") -> "video/quicktime"
            lower.contains(".3gp") -> "video/3gpp"
            lower.contains(".mp3") -> "audio/mpeg"
            lower.contains(".m4a") -> "audio/mp4"
            lower.contains(".aac") -> "audio/aac"
            lower.contains(".wav") -> "audio/wav"
            lower.contains(".ogg") || lower.contains(".opus") -> "audio/ogg"
            lower.contains(".mp4") || lower.contains(".m4v") -> "video/mp4"
            else -> "application/octet-stream"
        }
    }

    private fun labelFrom(url: String): String {
        val lower = url.lowercase(Locale.US)
        val quality = when {
            lower.contains("240p") -> "240p"
            lower.contains("360p") -> "360p"
            lower.contains("480p") -> "480p"
            lower.contains("720p") -> "720p"
            lower.contains("1080p") -> "1080p"
            lower.contains("144p") -> "144p"
            lower.contains("2160p") -> "2160p"
            else -> ""
        }
        val kind = when {
            lower.contains(".mp3") || lower.contains("audio") -> "Audio"
            lower.contains(".m4a") || lower.contains(".aac") || lower.contains(".ogg") || lower.contains(".opus") || lower.contains(".wav") -> "Audio"
            else -> "Video"
        }
        val ext = when {
            lower.contains(".mp3") -> "MP3"
            lower.contains(".m4a") -> "M4A"
            lower.contains(".aac") -> "AAC"
            lower.contains(".ogg") || lower.contains(".opus") -> "OGG"
            lower.contains(".wav") -> "WAV"
            lower.contains(".webm") -> "WEBM"
            lower.contains(".mkv") -> "MKV"
            lower.contains(".mov") -> "MOV"
            lower.contains(".3gp") -> "3GP"
            else -> "MP4"
        }
        return if (quality.isNotEmpty()) {
            "$kind $quality ($ext)"
        } else {
            "$kind ($ext)"
        }
    }
}

object CompliantVideoProvider : VideoFormatProvider {
    override fun lookup(context: Context, check: UrlCheck): ProviderResult {
        return when (check) {
            is UrlCheck.Bad -> ProviderResult.Failed(check.message)
            is UrlCheck.Social -> ProviderResult.Unavailable(
                check.url,
                "YouTube, YouTube Shorts, TikTok, Facebook, and Instagram do not give this app an authorized video or audio file. No quality list is shown and nothing is saved. Open the link in the browser or use a dedicated extractor service."
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
        val videoExts = setOf("mp4", "webm", "mkv", "mov", "m4v", "3gp")
        val audioExts = setOf("mp3", "m4a", "aac", "wav", "ogg", "opus")
        val kind = when {
            type.startsWith("video/") -> "video"
            type.startsWith("audio/") -> "audio"
            (type == "application/octet-stream" || type.isEmpty()) && info.ext in videoExts -> "video"
            (type == "application/octet-stream" || type.isEmpty()) && info.ext in audioExts -> "audio"
            else -> ""
        }
        if (kind.isEmpty()) {
            return ProviderResult.Failed("This link is not a direct video or audio file. YouTube, Shorts, TikTok, Facebook, and Instagram cannot be saved from here, so no format buttons are shown.")
        }
        val label = when {
            type.contains("webm") || info.ext == "webm" -> if (kind == "audio") "WEBM audio" else "WEBM video"
            type.contains("mpeg") || info.ext == "mp3" -> "MP3 audio"
            type.contains("aac") || info.ext == "aac" -> "AAC audio"
            info.ext == "m4a" || (kind == "audio" && (type.contains("mp4") || info.ext == "mp4")) -> "M4A audio"
            type.contains("ogg") || info.ext == "ogg" || info.ext == "opus" -> "OGG audio"
            type.contains("wav") || info.ext == "wav" -> "WAV audio"
            type.contains("matroska") || info.ext == "mkv" -> "MKV video"
            type.contains("quicktime") || info.ext == "mov" -> "MOV video"
            type.contains("3gpp") || info.ext == "3gp" -> "3GP video"
            kind == "audio" -> "Audio file"
            else -> "MP4 video"
        }
        val size = if (info.bytes > 0L) " (" + (info.bytes / 1024L) + " KB)" else ""
        val mimeOut = if (type.isNotEmpty()) type else if (kind == "audio") "audio/mpeg" else "video/mp4"
        return ProviderResult.Formats(
            info.finalUrl,
            listOf(VideoFormat(label + size, mimeOut, info.bytes, info.finalUrl))
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
            var bytes = conn.contentLengthLong
            val range = conn.getHeaderField("Content-Range")
            if (range != null && range.contains("/")) {
                val totalText = range.substring(range.lastIndexOf('/') + 1).trim()
                val total = totalText.toLongOrNull()
                if (total != null && total > 0L) bytes = total
            }
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
        req.addRequestHeader("User-Agent", "PDFCraftStudio")
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

    class Progress(val status: Int, val done: Long, val total: Long, val reason: Int, val localUri: String? = null)

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
                val localIdx = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val local = if (localIdx >= 0) it.getString(localIdx) else null
                Progress(st, done, total, reason, local)
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
            lower.endsWith(".mov") || lower.endsWith(".m4v") || lower.endsWith(".3gp") ||
            lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") ||
            lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".opus")
        if (!hasExt) {
            val ext = when {
                mime.contains("webm") -> ".webm"
                mime.contains("matroska") || mime.contains("mkv") -> ".mkv"
                mime.contains("quicktime") -> ".mov"
                mime.contains("3gpp") -> ".3gp"
                mime.contains("mpeg") || mime.contains("mp3") -> ".mp3"
                mime.contains("aac") -> ".aac"
                mime.contains("ogg") || mime.contains("opus") -> ".ogg"
                mime.contains("wav") -> ".wav"
                mime.startsWith("audio/") -> ".m4a"
                else -> ".mp4"
            }
            name = name + ext
        }
        return name
    }
}
