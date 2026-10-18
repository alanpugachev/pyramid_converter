package alanpugachev.pyramid_converter.service

import alanpugachev.pyramid_converter.enums.FileFormat
import nl.siegmann.epublib.epub.EpubReader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.kxml2.io.KXmlParser
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.nio.file.Path

class EpubToPdfService : ConversionService {
    override val from = FileFormat.Epub
    override val to = FileFormat.Pdf

    override fun convert(input: Path, output: Path) {
        val book = input.toFile().inputStream().use { EpubReader().readEpub(it) }

        val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)
        val lines = mutableListOf<String>()
        for (spineReference in book.spine.spineReferences) {
            val resource = spineReference.resource ?: continue
            val mediaType = resource.mediaType?.name ?: ""
            if ("html" !in mediaType) continue

            val text = htmlToText(String(resource.data, Charsets.UTF_8))
            if (text.isEmpty()) continue

            lines += wrap(sanitize(text), font, FONT_SIZE, CONTENT_WIDTH)
            lines += ""
        }

        PDDocument().use { document ->
            var index = 0
            while (index < lines.size) {
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)

                val pageLines = lines.subList(index, minOf(index + LINES_PER_PAGE, lines.size))
                PDPageContentStream(document, page).use { stream ->
                    stream.beginText()
                    stream.setFont(font, FONT_SIZE)
                    stream.newLineAtOffset(MARGIN, PDRectangle.A4.height - MARGIN)
                    for (line in pageLines) {
                        stream.showText(line)
                        stream.newLineAtOffset(0f, -LEADING)
                    }
                    stream.endText()
                }
                index += LINES_PER_PAGE
            }
            document.save(output.toFile())
        }
    }

    private fun htmlToText(html: String): String {
        val parser = KXmlParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(preprocess(html)))

        val text = StringBuilder()
        var skippedDepth = 0
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.lowercase()
                    when {
                        skippedDepth > 0 && tag in SKIPPED_TAGS -> skippedDepth++
                        skippedDepth == 0 && tag in SKIPPED_TAGS -> skippedDepth = 1
                        skippedDepth == 0 && tag in BLOCK_TAGS -> text.append('\n')
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.lowercase()
                    if (tag in SKIPPED_TAGS && skippedDepth > 0) skippedDepth--
                    if (skippedDepth == 0 && tag in BLOCK_TAGS) text.append('\n')
                }
                XmlPullParser.TEXT -> if (skippedDepth == 0) text.append(parser.text)
            }
        }

        return text.toString()
            .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
            .lineSequence()
            .map(String::trim)
            .joinToString("\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun preprocess(html: String): String =
        Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE).replace(html, "")
            .let(::replaceHtmlEntities)

    private fun replaceHtmlEntities(html: String): String =
        HTML_ENTITY_REGEX.replace(html) { match ->
            htmlEntities[match.groupValues[1]]?.let { "&#${it.code};" } ?: match.value
        }

    private fun wrap(text: String, font: PDFont, fontSize: Float, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        for (paragraph in text.split('\n')) {
            if (paragraph.isBlank()) {
                lines.add("")
                continue
            }
            var line = ""
            for (word in paragraph.split(' ')) {
                if (word.isEmpty()) continue
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (width(candidate, font, fontSize) <= maxWidth) {
                    line = candidate
                } else {
                    if (line.isNotEmpty()) lines.add(line)
                    if (width(word, font, fontSize) > maxWidth) {
                        var chunk = ""
                        for (ch in word) {
                            if (width(chunk + ch, font, fontSize) <= maxWidth) {
                                chunk += ch
                            } else {
                                lines.add(chunk)
                                chunk = ch.toString()
                            }
                        }
                        line = chunk
                    } else {
                        line = word
                    }
                }
            }
            if (line.isNotEmpty()) lines.add(line)
        }
        return lines
    }

    private fun width(text: String, font: PDFont, fontSize: Float): Float =
        font.getStringWidth(text) / 1000 * fontSize

    private fun sanitize(text: String): String = buildString {
        for (c in text) {
            append(
                when {
                    c == '\n' -> c
                    c.code in 32..126 || c.code in 160..255 || c.code in WIN_ANSI_EXTRA -> c
                    else -> '?'
                }
            )
        }
    }

    companion object {
        private const val FONT_SIZE = 11f
        private const val LEADING = 15.4f
        private const val MARGIN = 50f
        private val CONTENT_WIDTH = PDRectangle.A4.width - 2 * MARGIN
        private val LINES_PER_PAGE = ((PDRectangle.A4.height - 2 * MARGIN) / LEADING).toInt()

        private val BLOCK_TAGS = setOf(
            "p", "br", "h1", "h2", "h3", "h4", "h5", "h6",
            "div", "li", "blockquote", "section", "table", "tr",
        )
        private val SKIPPED_TAGS = setOf("head", "style", "script")

        private val HTML_ENTITY_REGEX = Regex("&([a-zA-Z][a-zA-Z0-9]*);")
        private val htmlEntities = mapOf(
            "nbsp" to ' ', "mdash" to '—', "ndash" to '–', "hellip" to '…',
            "lsquo" to '\u2018', "rsquo" to '\u2019', "ldquo" to '\u201C', "rdquo" to '\u201D',
            "copy" to '©', "reg" to '®', "trade" to '™',
            "agrave" to 'à', "auml" to 'ä', "ouml" to 'ö', "uuml" to 'ü',
            "eacute" to 'é', "egrave" to 'è', "szlig" to 'ß',
            "laquo" to '«', "raquo" to '»', "deg" to '°', "plusmn" to '±', "middot" to '·',
        )

        private val WIN_ANSI_EXTRA = setOf(
            8364, 8218, 402, 8222, 8230, 8224, 8225, 710, 8240, 352, 8249, 338, 381,
            8216, 8217, 8220, 8221, 8226, 8211, 8212, 732, 8482, 353, 8250, 339, 382, 376,
        )
    }
}
