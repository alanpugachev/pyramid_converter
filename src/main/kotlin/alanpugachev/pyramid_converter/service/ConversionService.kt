package alanpugachev.pyramid_converter.service

import alanpugachev.pyramid_converter.enums.FileFormat
import java.nio.file.Path

interface ConversionService {
    val from: FileFormat
    val to: FileFormat

    fun convert(input: Path, output: Path)
}
