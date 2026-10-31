package com.pdfcraft.studio.ui.viewer

// fix56: Word / Excel / PPT / CSV / Text readers without new libraries (java.util.zip + XmlPullParser)

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

internal class DocPara(val text: String, val heading: Int, val bold: List<IntRange>, val italic: List<IntRange>)
internal class DocSheet(val name: String, val rows: List<List<String>>)
internal class DocSlide(val number: Int, val lines: List<String>)

internal object DocParse {
    const val MAX_TEXT = 1_500_000
    const val MAX_ROWS = 5000
    const val MAX_COLS = 60

    private fun ln(p: XmlPullParser): String = p.name.substringAfterLast(':')
    private fun attr(p: XmlPullParser, local: String): String? {
        for (i in 0 until p.attributeCount) if (p.getAttributeName(i).substringAfterLast(':') == local) return p.getAttributeValue(i)
        return null
    }
    private fun parser(s: InputStream): XmlPullParser {
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        p.setInput(s, "UTF-8")
        return p
    }
    private fun on(p: XmlPullParser): Boolean {
        val v = attr(p, "val") ?: return true
        return v != "0" && v != "false" && v != "none"
    }

    fun readText(f: File): String {
        val bytes = f.inputStream().use { s ->
            val b = ByteArray(minOf(f.length(), MAX_TEXT.toLong()).toInt().coerceAtLeast(0))
            var n = 0
            while (n < b.size) { val r = s.read(b, n, b.size - n); if (r <= 0) break; n += r }
            if (n < b.size) b.copyOf(n) else b
        }
        val t = String(bytes, Charsets.UTF_8)
        return if (t.isNotEmpty() && t[0] == '\uFEFF') t.substring(1) else t
    }

    fun docx(f: File): List<DocPara> {
        val out = ArrayList<DocPara>()
        ZipFile(f).use { z ->
            val e = z.getEntry("word/document.xml") ?: return out
            z.getInputStream(e).use { s ->
                val p = parser(s)
                val sb = StringBuilder()
                val bold = ArrayList<IntRange>()
                val ital = ArrayList<IntRange>()
                var head = 0
                var inP = false
                var inRpr = false
                var rb = false
                var ri = false
                var inT = false
                var total = 0
                var ev = p.eventType
                while (ev != XmlPullParser.END_DOCUMENT && total < MAX_TEXT) {
                    when (ev) {
                        XmlPullParser.START_TAG -> when (ln(p)) {
                            "p" -> { inP = true; sb.setLength(0); bold.clear(); ital.clear(); head = 0 }
                            "pStyle" -> {
                                val v = (attr(p, "val") ?: "").lowercase()
                                head = when {
                                    v.contains("title") -> 1
                                    v.startsWith("heading") -> (v.removePrefix("heading").trim().toIntOrNull() ?: 1).coerceIn(1, 6)
                                    else -> head
                                }
                            }
                            "r" -> { rb = false; ri = false }
                            "rPr" -> { inRpr = true }
                            "b" -> { if (inRpr) rb = on(p) }
                            "i" -> { if (inRpr) ri = on(p) }
                            "t" -> { inT = true }
                            "tab" -> { if (inP) sb.append('\t') }
                            "br", "cr" -> { if (inP) sb.append('\n') }
                        }
                        XmlPullParser.END_TAG -> when (ln(p)) {
                            "rPr" -> { inRpr = false }
                            "t" -> { inT = false }
                            "tc" -> { if (inP) sb.append("  ") }
                            "p" -> {
                                if (inP) {
                                    out.add(DocPara(sb.toString(), head, ArrayList(bold), ArrayList(ital)))
                                    total += sb.length
                                }
                                inP = false
                            }
                        }
                        XmlPullParser.TEXT -> { if (inT && inP) { }
                            val a = sb.length
                            sb.append(p.text ?: "")
                            if (sb.length > a) {
                                if (rb) bold.add(a until sb.length)
                                if (ri) ital.add(a until sb.length)
                            }
                        }
                    }
                    ev = p.next()
                }
            }
        }
        return out
    }

    private fun colIndex(ref: String): Int {
        var c = 0
        for (ch in ref) { if (ch in 'A'..'Z') c = c * 26 + (ch - 'A' + 1) else if (ch in 'a'..'z') c = c * 26 + (ch - 'a' + 1) else break }
        return (c - 1).coerceAtLeast(0)
    }

    fun xlsx(f: File): List<DocSheet> {
        val sheets = ArrayList<DocSheet>()
        ZipFile(f).use { z ->
            val shared = ArrayList<String>()
            z.getEntry("xl/sharedStrings.xml")?.let { e ->
                z.getInputStream(e).use { s ->
                    val p = parser(s)
                    val sb = StringBuilder()
                    var inSi = false
                    var inT = false
                    var inRph = false
                    var ev = p.eventType
                    while (ev != XmlPullParser.END_DOCUMENT) {
                        when (ev) {
                            XmlPullParser.START_TAG -> when (ln(p)) { "si" -> { inSi = true; sb.setLength(0) }; "t" -> { inT = true }; "rPh" -> { inRph = true } }
                            XmlPullParser.END_TAG -> when (ln(p)) { "si" -> { shared.add(sb.toString()); inSi = false }; "t" -> { inT = false }; "rPh" -> { inRph = false } }
                            XmlPullParser.TEXT -> { if (inSi && inT && !inRph) sb.append(p.text ?: "") }
                        }
                        ev = p.next()
                    }
                }
            }
            val rels = HashMap<String, String>()
            z.getEntry("xl/_rels/workbook.xml.rels")?.let { e ->
                z.getInputStream(e).use { s ->
                    val p = parser(s)
                    var ev = p.eventType
                    while (ev != XmlPullParser.END_DOCUMENT) {
                        if (ev == XmlPullParser.START_TAG && ln(p) == "Relationship") {
                            val id = attr(p, "Id"); val t = attr(p, "Target")
                            if (id != null && t != null) rels[id] = t
                        }
                        ev = p.next()
                    }
                }
            }
            val order = ArrayList<Pair<String, String>>()
            z.getEntry("xl/workbook.xml")?.let { e ->
                z.getInputStream(e).use { s ->
                    val p = parser(s)
                    var ev = p.eventType
                    while (ev != XmlPullParser.END_DOCUMENT) {
                        if (ev == XmlPullParser.START_TAG && ln(p) == "sheet") {
                            val name = attr(p, "name") ?: ("Sheet" + (order.size + 1))
                            val t = rels[attr(p, "id") ?: ""]
                            if (t != null) {
                                val path = if (t.startsWith("/")) t.removePrefix("/") else "xl/" + t.removePrefix("./")
                                order.add(name to path)
                            }
                        }
                        ev = p.next()
                    }
                }
            }
            if (order.isEmpty()) {
                var i = 1
                while (z.getEntry("xl/worksheets/sheet" + i + ".xml") != null) { order.add(("Sheet" + i) to ("xl/worksheets/sheet" + i + ".xml")); i++ }
            }
            for ((name, path) in order) {
                val e = z.getEntry(path) ?: continue
                val rows = ArrayList<List<String>>()
                z.getInputStream(e).use { s ->
                    val p = parser(s)
                    var row: ArrayList<String>? = null
                    var rowNum = 0
                    var col = 0
                    var type = ""
                    var inV = false
                    val v = StringBuilder()
                    var ev = p.eventType
                    while (ev != XmlPullParser.END_DOCUMENT && rows.size < MAX_ROWS) {
                        when (ev) {
                            XmlPullParser.START_TAG -> when (ln(p)) {
                                "row" -> {
                                    val r = attr(p, "r")?.toIntOrNull() ?: (rowNum + 1)
                                    while (rowNum < r - 1 && rows.size < MAX_ROWS) { rows.add(emptyList()); rowNum++ }
                                    row = ArrayList(); rowNum = r
                                }
                                "c" -> { col = attr(p, "r")?.let { colIndex(it) } ?: (row?.size ?: 0); type = attr(p, "t") ?: ""; v.setLength(0) }
                                "v", "t" -> { inV = true }
                            }
                            XmlPullParser.END_TAG -> when (ln(p)) {
                                "v", "t" -> { inV = false }
                                "c" -> {
                                    val r = row
                                    if (r != null && col < MAX_COLS) {
                                        val raw = v.toString()
                                        val text = when (type) {
                                            "s" -> shared.getOrNull(raw.trim().toIntOrNull() ?: -1) ?: ""
                                            "b" -> { if (raw.trim() == "1") "TRUE" else "FALSE" }
                                            else -> raw
                                        }
                                        while (r.size < col) r.add("")
                                        if (r.size == col) r.add(text) else r[col] = text
                                    }
                                }
                                "row" -> { row?.let { rows.add(it) }; row = null }
                            }
                            XmlPullParser.TEXT -> { if (inV) v.append(p.text ?: "") }
                        }
                        ev = p.next()
                    }
                }
                sheets.add(DocSheet(name, rows))
            }
        }
        return sheets
    }

    fun csv(f: File): DocSheet {
        val t = readText(f)
        val first = t.lineSequence().firstOrNull() ?: ""
        val sep = listOf(',', ';', '\t').maxByOrNull { c -> { first.count { it == c } } ?: ',' }
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var q = false
        var i = 0
        while (i < t.length && rows.size < MAX_ROWS) {
            val ch = t[i]
            if (q) {
                if (ch == '"') { if (i + 1 < t.length && t[i + 1] == '"') { cell.append('"'); i++ } else q = false } else cell.append(ch)
            } else when (ch) {
                '"' -> { q = true }
                sep -> { row.add(cell.toString()); cell.setLength(0) }
                '\r' -> {}
                '\n' -> { row.add(cell.toString()); cell.setLength(0); rows.add(row.take(MAX_COLS)); row = ArrayList() }
                else -> cell.append(ch)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row.take(MAX_COLS)) }
        return DocSheet(f.nameWithoutExtension, rows)
    }

    fun pptx(f: File): List<DocSlide> {
        val out = ArrayList<DocSlide>()
        ZipFile(f).use { z ->
            val names = ArrayList<Pair<Int, String>>()
            val en = z.entries()
            while (en.hasMoreElements()) {
                val n = en.nextElement().name
                if (n.startsWith("ppt/slides/slide") && n.endsWith(".xml")) {
                    val num = n.removePrefix("ppt/slides/slide").removeSuffix(".xml").toIntOrNull()
                    if (num != null) names.add(num to n)
                }
            }
            for ((num, n) in names.sortedBy { it.first }) {
                val lines = ArrayList<String>()
                z.getInputStream(z.getEntry(n)).use { s ->
                    val p = parser(s)
                    val sb = StringBuilder()
                    var inT = false
                    var ev = p.eventType
                    while (ev != XmlPullParser.END_DOCUMENT) {
                        when (ev) {
                            XmlPullParser.START_TAG -> when (ln(p)) { "p" -> { sb.setLength(0) }; "t" -> { inT = true }; "br" -> { sb.append('\n') } }
                            XmlPullParser.END_TAG -> when (ln(p)) {
                                "t" -> { inT = false }
                                "p" -> { val l = sb.toString().trim(); if (l.isNotEmpty()) lines.add(l) }
                            }
                            XmlPullParser.TEXT -> { if (inT) sb.append(p.text ?: "") }
                        }
                        ev = p.next()
                    }
                }
                out.add(DocSlide(num, lines))
            }
        }
        return out
    }
}
