package alanpugachev.pyramid_converter.service

import alanpugachev.pyramid_converter.enums.FileFormat

class ConversionRouter(services: List<ConversionService>) {
    private val servicesByKey: Map<Pair<FileFormat, FileFormat>, ConversionService> =
        services.associateBy { it.from to it.to }

    fun serviceFor(from: FileFormat, to: FileFormat): ConversionService =
        servicesByKey[from to to]
            ?: throw UnsupportedOperationException(
                "Unsupported conversion: ${from.displayName} -> ${to.displayName}"
            )
}
