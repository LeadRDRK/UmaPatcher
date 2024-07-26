package com.leadrdrk.umapatcher.zip

import kotlinx.coroutines.delay
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.AbstractFileHeader
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File

class ZipFileExtractor(
    file: File
): ZipExtractor(file) {
    private val zip = ZipFile(file).apply {
        isRunInThread = true
    }

    override suspend fun extractAll(dir: File, progressCallback: (Float) -> Unit) {
        zip.extractAll(dir.path)
        val progressMonitor = zip.progressMonitor
        while (!progressMonitor.state.equals(ProgressMonitor.State.READY)) {
            progressCallback(progressMonitor.percentDone / 100f)
            delay(100)
        }
    }

    override val fileHeaders: List<AbstractFileHeader>
        get() = zip.fileHeaders

    override fun close() {
        zip.close()
    }
}