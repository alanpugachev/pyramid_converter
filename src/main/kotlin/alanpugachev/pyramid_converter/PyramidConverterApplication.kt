package alanpugachev.pyramid_converter

import alanpugachev.pyramid_converter.enums.FileFormat
import alanpugachev.pyramid_converter.service.ConversionRouter
import alanpugachev.pyramid_converter.service.EpubToPdfService
import alanpugachev.pyramid_converter.service.PdfToEpubService
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path

fun main() {
    application {
        val router = remember { ConversionRouter(listOf(PdfToEpubService(), EpubToPdfService())) }
        val scope = rememberCoroutineScope()
        var selectedFile by remember { mutableStateOf<String?>(null) }
        var status by remember { mutableStateOf("") }
        var isConverting by remember { mutableStateOf(false) }

        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(width = 520.dp, height = 320.dp),
            title = "Pyramid Converter",
        ) {
            MaterialTheme {
                if (selectedFile == null) {
                    StartScreen(
                        onFileChosen = { path ->
                            selectedFile = path
                            status = ""
                        }
                    )
                } else {
                    ConverterScreen(
                        filePath = selectedFile!!,
                        onChangeFile = {
                            selectedFile = null
                            status = ""
                        },
                        status = status,
                        isConverting = isConverting,
                        onConvert = {
                            val detected = FileFormat.fromFile(selectedFile!!)!!
                            val input = Path.of(selectedFile!!)
                            val output = input.resolveSibling(
                                input.fileName.toString().substringBeforeLast('.') +
                                    "." + detected.target.extensions.first()
                            )
                            scope.launch(Dispatchers.IO) {
                                isConverting = true
                                status = "Converting..."
                                try {
                                    router.serviceFor(detected, detected.target).convert(input, output)
                                    status = "Saved: ${output.fileName}"
                                } catch (e: NotImplementedError) {
                                    status = "${detected.target.displayName} conversion is not implemented yet"
                                } catch (e: Exception) {
                                    status = "Error: ${e.message}"
                                } finally {
                                    isConverting = false
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StartScreen(onFileChosen: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Button(onClick = { chooseFile(onFileChosen) }) {
            Text("Choose File")
        }
    }
}

@Composable
private fun ConverterScreen(
    filePath: String,
    onChangeFile: () -> Unit,
    status: String,
    isConverting: Boolean,
    onConvert: () -> Unit,
) {
    val fileName = filePath.substringAfterLast('/')
    val detectedFormat = FileFormat.fromFile(filePath)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            fileName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )

        if (detectedFormat == null) {
            Text(
                "Unknown or unsupported format",
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            Text("Detected format: ${detectedFormat.displayName}")

            Button(
                onClick = onConvert,
                enabled = !isConverting,
            ) {
                Text("Convert to ${detectedFormat.target.displayName}")
            }
            if (status.isNotEmpty()) {
                Text(status)
            }
        }

        TextButton(onClick = onChangeFile) {
            Text("Choose Another File")
        }
    }
}

private fun chooseFile(onChosen: (String) -> Unit) {
    val dialog = FileDialog(null as Frame?, "Open", FileDialog.LOAD)
    dialog.isVisible = true
    val name = dialog.file ?: return
    onChosen(dialog.directory + name)
}
