package com.pdfcraft.studio.ui.viewer

// fix56: free on-device Text to Speech (Android TextToSpeech) with a voice picker

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

internal class TtsStyle(val name: String, val emoji: String, val pitch: Float, val rate: Float)

internal val TTS_STYLES = listOf(
    TtsStyle("Normal", "\uD83D\uDE42", 1f, 1f),
    TtsStyle("Male (deep)", "\uD83D\uDC68", 0.72f, 0.95f),
    TtsStyle("Female", "\uD83D\uDC69", 1.25f, 1f),
    TtsStyle("Child", "\uD83E\uDDD2", 1.75f, 1.1f),
    TtsStyle("Old", "\uD83D\uDC74", 0.8f, 0.78f),
    TtsStyle("Robot", "\uD83E\uDD16", 0.45f, 0.85f),
    TtsStyle("Cartoon", "\uD83D\uDC2D", 2f, 1.3f),
    TtsStyle("Slow", "\uD83D\uDC22", 1f, 0.65f),
    TtsStyle("Fast", "\u26A1", 1f, 1.6f)
)

/** fix56: one TextToSpeech for a viewer. Long text is spoken in chunks. */
internal class DocTts(ctx: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(ctx.applicationContext, this)
    var ready by mutableStateOf(false)
    var failed by mutableStateOf(false)
    var speaking by mutableStateOf(false)
    var paused by mutableStateOf(false)
    var voices by mutableStateOf<List<Voice>>(emptyList())
    var current by mutableStateOf<String?>(null)
    var part by mutableIntStateOf(0)
    var parts by mutableIntStateOf(0)
    private var chunks: List<String> = emptyList()

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) { failed = true; return }
        ready = true
        voices = try {
            (tts.voices ?: emptySet()).filter { !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
                .sortedWith(compareBy({ it.locale.displayLanguage }, { it.locale.displayCountry }, { it.name }))
        } catch (e: Exception) { emptyList() }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { id?.removePrefix("c")?.toIntOrNull()?.let { part = it } }
            override fun onDone(id: String?) {
                val i = id?.removePrefix("c")?.toIntOrNull() ?: return
                if (i >= chunks.size - 1 && !paused) speaking = false
            }
            @Deprecated("old api")
            override fun onError(id: String?) { speaking = false }
        })
    }

    private fun split(text: String): List<String> {
        val max = (try { TextToSpeech.getMaxSpeechInputLength() } catch (e: Exception) { 4000 }).coerceIn(200, 3900) - 50
        val out = ArrayList<String>()
        var i = 0
        while (i < text.length) {
            var end = minOf(text.length, i + max)
            if (end < text.length) {
                val cut = maxOf(text.lastIndexOf(". ", end), text.lastIndexOf('\n', end), text.lastIndexOf("? ", end), text.lastIndexOf("! ", end))
                if (cut > i + max / 3) end = cut + 1 else { val sp = text.lastIndexOf(' ', end); if (sp > i + max / 3) end = sp + 1 }
            }
            val c = text.substring(i, end).trim()
            if (c.isNotEmpty()) out.add(c)
            i = end
        }
        return out
    }

    private fun speakFrom(start: Int) {
        for (k in start until chunks.size) {
            tts.speak(chunks[k], if (k == start) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "c" + k)
        }
    }

    fun speak(text: String, voice: Voice?, style: TtsStyle) {
        if (!ready) return
        tts.stop()
        try { if (voice != null) tts.voice = voice } catch (e: Exception) { }
        tts.setPitch(style.pitch)
        tts.setSpeechRate(style.rate)
        current = voice?.name
        chunks = split(text)
        parts = chunks.size
        part = 0
        paused = false
        if (chunks.isEmpty()) { speaking = false; return }
        speaking = true
        speakFrom(0)
    }

    fun pause() { if (!speaking) return; paused = true; tts.stop() }
    fun resume() { if (!paused) return; paused = false; speaking = true; speakFrom(part.coerceIn(0, (chunks.size - 1).coerceAtLeast(0))) }
    fun stop() { paused = false; speaking = false; tts.stop() }
    fun release() { try { tts.stop(); tts.shutdown() } catch (e: Exception) { } }
}

private fun genderOf(v: Voice): String? {
    val n = v.name.lowercase()
    val f = (try { v.features } catch (e: Exception) { null }) ?: emptySet<String>()
    val all = n + " " + f.joinToString(" ").lowercase()
    return when {
        all.contains("female") -> "Female"
        all.contains("male") -> "Male"
        else -> null
    }
}

internal fun voiceTitle(v: Voice): String {
    val l = v.locale
    return l.displayLanguage + (if (l.displayCountry.isNotEmpty()) " (" + l.displayCountry + ")" else "")
}

internal fun voiceTags(v: Voice): List<String> {
    val t = ArrayList<String>()
    genderOf(v)?.let { t.add(it) }
    t.add(if (v.isNetworkConnectionRequired) "Online" else "Offline")
    t.add(when {
        v.quality >= Voice.QUALITY_VERY_HIGH -> "HD+"
        v.quality >= Voice.QUALITY_HIGH -> "HD"
        v.quality >= Voice.QUALITY_NORMAL -> "Standard"
        else -> "Basic"
    })
    return t
}

@Composable
internal fun SpeakerIcon(color: Color) { Text("\uD83D\uDD0A", fontSize = 20.sp, color = color) }

@Composable
private fun RoundBtn(label: String, bg: Color, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

/** fix56: bottom sheet: voice styles, voice list, play/pause/stop. [selection] = true when only selected text is spoken. */
@Composable
internal fun TtsSheet(tts: DocTts, selection: Boolean, onPick: (Voice?, TtsStyle) -> Unit, onClose: () -> Unit) {
    var style by remember { mutableIntStateOf(0) }
    var mine by remember { mutableStateOf(true) }
    val lang = Locale.getDefault().language
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0x66000000)).clickable(onClick = onClose), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = maxHeight * 0.75f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Color.White)
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 4.dp).clip(CircleShape).background(Color(0xFFDDDDDD)))
            Spacer(Modifier.height(10.dp))
            Text("Read aloud", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DocInk)
            Text(
                if (selection) "Only your selected text will be read." else "The whole text will be read from start to end. Select some text first to read only that part.",
                fontSize = 13.sp, color = DocSub
            )
            Spacer(Modifier.height(10.dp))
            Text("Voice style", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DocSub)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TTS_STYLES.forEachIndexed { i, s ->
                    val on = i == style
                    Text(
                        s.emoji + " " + s.name, fontSize = 13.sp, color = if (on) Color.White else DocInk,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) DocRed else Color(0xFFF2F2F5))
                            .clickable { style = i }.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            if (tts.speaking || tts.paused) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    if (tts.paused) RoundBtn("\u25B6", Color(0xFF43A047)) { tts.resume() } else RoundBtn("\u275A\u275A", Color(0xFFFB8C00)) { tts.pause() }
                    RoundBtn("\u25A0", DocRed) { tts.stop() }
                    Text(
                        (if (tts.paused) "Paused" else "Speaking") + (if (tts.parts > 1) "  \u2022  part " + (tts.part + 1) + " / " + tts.parts else ""),
                        color = DocInk, fontSize = 14.sp
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Voices", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DocSub, modifier = Modifier.weight(1f))
                Text("My language", fontSize = 12.sp, color = DocSub)
                Spacer(Modifier.width(6.dp))
                Switch(checked = mine, onCheckedChange = { mine = it })
            }
            when {
                tts.failed -> Text("Text to speech is not available. Install or enable \"Speech Services by Google\" in your phone settings.", color = DocRed, fontSize = 13.sp)
                !tts.ready -> Text("Loading voices\u2026", color = DocSub, fontSize = 13.sp)
                else -> {
                    val list = tts.voices.filter { !mine || it.locale.language == lang }.ifEmpty { tts.voices }
                    LazyColumn(Modifier.fillMaxWidth()) {
                        item {
                            VoiceRow("Default voice", listOf("Phone setting"), tts.current == null && tts.speaking) { onPick(null, TTS_STYLES[style]) }
                        }
                        items(list, key = { it.name }) { v ->
                            VoiceRow(voiceTitle(v), voiceTags(v) + v.name.substringAfter("-x-", "").substringBefore('-').let { if (it.isEmpty()) emptyList() else listOf(it) },
                                tts.current == v.name && tts.speaking) { onPick(v, TTS_STYLES[style]) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceRow(title: String, tags: List<String>, active: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp))
            .background(if (active) Color(0xFFFFEBEE) else Color(0xFFF8F8FA))
            .border(1.dp, if (active) DocRed else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(if (active) DocRed else Color(0xFFE8E8EE)), contentAlignment = Alignment.Center) {
            Text(if (active) "\uD83D\uDD0A" else "\u25B6", fontSize = 14.sp, color = if (active) Color.White else DocInk)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = DocInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tags.joinToString("  \u2022  "), color = DocSub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
