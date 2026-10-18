package alanpugachev.pyramid_converter.service

import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubWriter
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

class EpubToPdfServiceTest {

    @Test
    fun `converts epub to pdf with extracted text`() {
        val tempDir = Files.createTempDirectory("pc_test")
        val epub = tempDir.resolve("sample.epub")
        val pdf = tempDir.resolve("sample.pdf")

        val chapter = """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head><title>Chapter 1</title></head>
            <body>
            <p>Hello EPUB to PDF</p>
            <p>Second paragraph &mdash; with an entity.</p>
            </body>
            </html>
        """.trimIndent()

        val book = Book().apply {
            metadata.addTitle("Sample Book")
            addSection("Chapter 1", Resource(chapter.toByteArray(Charsets.UTF_8), "chapter1.xhtml"))
        }
        EpubWriter().write(book, epub.toFile().outputStream())

        EpubToPdfService().convert(epub, pdf)

        assertTrue(Files.exists(pdf), "PDF file should be created")
        Loader.loadPDF(pdf.toFile()).use { document ->
            val extracted = PDFTextStripper().getText(document)
            assertTrue("Hello EPUB to PDF" in extracted, "PDF should contain chapter text. Was: $extracted")
            assertTrue("Second paragraph — with an entity." in extracted)
        }
    }

    @Test
    fun `wraps long paragraphs across lines`() {
        val tempDir = Files.createTempDirectory("pc_test")
        val epub = tempDir.resolve("long.epub")
        val pdf = tempDir.resolve("long.pdf")

        val longText = buildString { repeat(200) { append("word ") } }
        val chapter = """
            <?xml version="1.0" encoding="utf-8"?>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <body><p>$longText</p></body>
            </html>
        """.trimIndent()

        val book = Book().apply {
            metadata.addTitle("Long Book")
            addSection("Chapter 1", Resource(chapter.toByteArray(Charsets.UTF_8), "chapter1.xhtml"))
        }
        EpubWriter().write(book, epub.toFile().outputStream())

        EpubToPdfService().convert(epub, pdf)

        Loader.loadPDF(pdf.toFile()).use { document ->
            val extracted = PDFTextStripper().getText(document)
            assertTrue(extracted.contains("word word word"), "Text should be present. Was: ${extracted.take(200)}")
            assertTrue("word1 " !in extracted, "Words should not be truncated")
        }
    }
}
