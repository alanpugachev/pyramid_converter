package alanpugachev.pyramid_converter.service

import alanpugachev.pyramid_converter.enums.FileFormat
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubWriter
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.nio.file.Path

class PdfToEpubService : ConversionService {
    override val from = FileFormat.Pdf
    override val to = FileFormat.Epub

    override fun convert(input: Path, output: Path) {
        Loader.loadPDF(input.toFile()).use { document ->
            val stripper = PDFTextStripper()
            val book = Book().apply {
                metadata.addTitle(documentTitle(document, input))
            }

            for (pageNumber in 1..document.numberOfPages) {
                stripper.startPage = pageNumber
                stripper.endPage = pageNumber
                val pageText = stripper.getText(document).trim()
                if (pageText.isEmpty()) continue

                book.addSection(
                    "Page $pageNumber",
                    Resource(pageXhtml(pageNumber, pageText).toByteArray(Charsets.UTF_8), "page_$pageNumber.xhtml"),
                )
            }

            EpubWriter().write(book, output.toFile().outputStream())
        }
    }

    private fun documentTitle(document: PDDocument, input: Path): String =
        document.documentInformation?.title?.takeIf { it.isNotBlank() }
            ?: input.fileName.toString().substringBeforeLast('.')

    private fun pageXhtml(pageNumber: Int, text: String): String {
        val paragraphs = text.lineSequence()
            .map(::escapeXml)
            .joinToString("\n") { "<p>$it</p>" }
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head><title>Page $pageNumber</title></head>
            <body>
            $paragraphs
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeXml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
