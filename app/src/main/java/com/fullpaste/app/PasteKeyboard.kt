package com.fullpaste.app

import android.content.ClipboardManager
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class PasteKeyboard : InputMethodService() {
    private var shift = false

    override fun onCreateInputView(): View {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(12, 12, 12, 12)
        root.setBackgroundColor(0xFFF3EFE6.toInt())
        val status = TextView(this)
        status.text = "Paste full writes the whole clipboard."
        status.setTextColor(0xFF1C1915.toInt())
        root.addView(status)
        val paste = key("Paste full")
        paste.setOnClickListener { pasteAll(status) }
        root.addView(paste, LinearLayout.LayoutParams(-1, dp(52)))
        for (row in listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")) {
            val line = LinearLayout(this)
            for (ch in row) {
                val button = key(ch.toString())
                button.setOnClickListener {
                    val out = if (shift) ch.uppercase() else ch.toString()
                    currentInputConnection?.commitText(out, 1)
                    shift = false
                }
                line.addView(button, LinearLayout.LayoutParams(0, dp(46), 1f))
            }
            root.addView(line)
        }
        val bottom = LinearLayout(this)
        val shiftKey = key("Shift")
        shiftKey.setOnClickListener { shift = !shift }
        val space = key("Space")
        space.setOnClickListener { currentInputConnection?.commitText(" ", 1) }
        val del = key("Del")
        del.setOnClickListener { currentInputConnection?.deleteSurroundingText(1, 0) }
        val enter = key("Enter")
        enter.setOnClickListener { currentInputConnection?.commitText("\n", 1) }
        val other = key("Other")
        other.setOnClickListener { getSystemService(InputMethodManager::class.java)?.showInputMethodPicker() }
        for (button in listOf(shiftKey, other, space, del, enter)) {
            bottom.addView(button, LinearLayout.LayoutParams(0, dp(46), 1f))
        }
        root.addView(bottom)
        return root
    }

    private fun pasteAll(status: TextView) {
        val ic = currentInputConnection
        if (ic == null) {
            status.text = "Tap the message box first."
            return
        }
        val clip = getSystemService(ClipboardManager::class.java)?.primaryClip
        val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(this)?.toString().orEmpty() else ""
        if (text.isEmpty()) {
            status.text = "Clipboard is empty."
            return
        }
        val handler = android.os.Handler(mainLooper)
        var index = 0
        fun next() {
            val input = currentInputConnection ?: return
            if (index >= text.length) {
                status.text = "Wrote " + text.length + " characters."
                return
            }
            val end = minOf(index + 50000, text.length)
            input.commitText(text.substring(index, end), 1)
            index = end
            handler.post { next() }
        }
        next()
    }

    private fun key(label: String): Button {
        val button = Button(this)
        button.text = label
        button.isAllCaps = false
        button.textSize = 14f
        return button
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
