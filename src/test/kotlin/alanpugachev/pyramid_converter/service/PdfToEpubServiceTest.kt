package alanpugachev.pyramid_converter.service

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfToEpubServiceTest {

    @Test
    fun `converts text pdf to epub`() {
        val tempDir = Files.createTempDirectory("pc_test")
        val pdf = tempDir.resolve("sample.pdf")
        val epub = tempDir.resolve("sample.epub")

        PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)
            PDPageContentStream(document, page).use { stream ->
                stream.beginText()
                stream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
                stream.newLineAtOffset(50f, 700f)
                stream.showText("Hello PDF to EPUB")
                stream.endText()
            }
            document.save(pdf.toFile())
        }

        PdfToEpubService().convert(pdf, epub)

        assertTrue(Files.exists(epub), "EPUB file should be created")
        ZipFile(epub.toFile()).use { zip ->
            assertEquals(
                "application/epub+zip",
                zip.getInputStream(zip.getEntry("mimetype")).readBytes().decodeToString(),
            )
            assertTrue(
                zip.entries().asSequence().any { it.name.endsWith("page_1.xhtml") },
                "EPUB should contain a page chapter",
            )
        }
    }
}
