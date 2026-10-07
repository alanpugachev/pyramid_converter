package alanpugachev.pyramid_converter.enums

enum class FileFormat(val displayName: String, val extensions: Set<String>) {
    Pdf("PDF", setOf("pdf")),
    Epub("EPUB", setOf("epub"));

    val target: FileFormat
        get() = when (this) {
            Pdf -> Epub
            Epub -> Pdf
        }

    companion object {
        fun fromFile(path: String): FileFormat? {
            val extension = path.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { extension in it.extensions }
        }
    }
}
