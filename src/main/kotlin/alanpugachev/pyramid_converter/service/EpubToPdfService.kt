package alanpugachev.pyramid_converter.service

import alanpugachev.pyramid_converter.enums.FileFormat
import java.nio.file.Path

class EpubToPdfService : ConversionService {
    override val from = FileFormat.Epub
    override val to = FileFormat.Pdf

    override fun convert(input: Path, output: Path) {
        throw NotImplementedError("EPUB -> PDF conversion is not implemented yet")
    }
}
