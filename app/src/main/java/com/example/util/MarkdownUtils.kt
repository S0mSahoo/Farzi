package com.example.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Parses markdown formatting (headers ###, bold **, italics *, bullets) into an AnnotatedString
 * for Jetpack Compose Text.
 */
fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()
        lines.forEachIndexed { lineIndex, line ->
            var lineText = line

            // Parse headers
            var isHeader = false
            if (lineText.startsWith("### ")) {
                lineText = lineText.removePrefix("### ")
                isHeader = true
            } else if (lineText.startsWith("## ")) {
                lineText = lineText.removePrefix("## ")
                isHeader = true
            } else if (lineText.startsWith("# ")) {
                lineText = lineText.removePrefix("# ")
                isHeader = true
            }

            // Format bullets (* or -)
            val trimmed = lineText.trimStart()
            if (trimmed.startsWith("* ") || trimmed.startsWith("- ")) {
                val leadingSpaces = lineText.takeWhile { it.isWhitespace() }
                val bulletContent = trimmed.drop(2)
                lineText = "${leadingSpaces}• $bulletContent"
            }

            if (isHeader) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendInlineFormatted(lineText)
                pop()
            } else {
                appendInlineFormatted(lineText)
            }

            if (lineIndex < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.appendInlineFormatted(text: String) {
    // Match bold **text**
    val boldRegex = Regex("""\*\*(.*?)\*\*""")
    var lastIndex = 0

    boldRegex.findAll(text).forEach { matchResult ->
        val range = matchResult.range
        if (range.first > lastIndex) {
            appendPlainWithItalics(text.substring(lastIndex, range.first))
        }
        val boldContent = matchResult.groupValues[1]
        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
        appendPlainWithItalics(boldContent)
        pop()
        lastIndex = range.last + 1
    }

    if (lastIndex < text.length) {
        appendPlainWithItalics(text.substring(lastIndex))
    }
}

private fun AnnotatedString.Builder.appendPlainWithItalics(text: String) {
    // Match italic *text* or _text_
    val italicRegex = Regex("""(?:\*|_)(.*?)(?:\*|_)""")
    var lastIndex = 0

    italicRegex.findAll(text).forEach { matchResult ->
        val range = matchResult.range
        if (range.first > lastIndex) {
            appendCleaned(text.substring(lastIndex, range.first))
        }
        val italicContent = matchResult.groupValues[1]
        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
        appendCleaned(italicContent)
        pop()
        lastIndex = range.last + 1
    }

    if (lastIndex < text.length) {
        appendCleaned(text.substring(lastIndex))
    }
}

private fun AnnotatedString.Builder.appendCleaned(text: String) {
    // Clean any stray markdown symbols that weren't matched
    val cleaned = text
        .replace("###", "")
        .replace("**", "")
    append(cleaned)
}
