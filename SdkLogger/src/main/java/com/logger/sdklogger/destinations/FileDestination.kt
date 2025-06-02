package com.logger.sdklogger.destinations

import com.logger.sdklogger.core.interfaces.LogDestination
import com.logger.sdklogger.core.interfaces.LogFormatter
import com.logger.sdklogger.core.models.LogEntry
import com.logger.sdklogger.formatters.DefaultLogFormatter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * File destination for logging to files with rotation
 */
class FileDestination(
    private val logDirectory: File,
    private val baseFileName: String = "sdk_logs",
    private val maxFileSize: Long = 10 * 1024 * 1024L, // 10MB
    private val maxFiles: Int = 5,
    private val formatter: LogFormatter = DefaultLogFormatter()
) : LogDestination {

    private val mutex = Mutex()
    private var currentFile: File? = null
    private var currentWriter: FileWriter? = null
    private var currentFileSize = 0L

    init {
        if (!logDirectory.exists()) {
            logDirectory.mkdirs()
        }
        initializeCurrentFile()
    }

    private fun initializeCurrentFile() {
        currentFile = File(logDirectory, "$baseFileName.log")
        currentFileSize = currentFile?.length() ?: 0L
    }

    override suspend fun writeLog(logEntry: LogEntry) = mutex.withLock {
        try {
            checkAndRotateFile()

            val formattedLog = formatter.format(logEntry) + "\n"
            val writer = getOrCreateWriter()

            writer.write(formattedLog)
            writer.flush()

            currentFileSize += formattedLog.length
        } catch (e: IOException) {
            // Handle file write error
            e.printStackTrace()
        }
    }

    private fun checkAndRotateFile() {
        if (currentFileSize >= maxFileSize) {
            rotateFiles()
        }
    }

    private fun rotateFiles() {
        try {
            currentWriter?.close()
            currentWriter = null

            // Rotate existing files
            for (i in maxFiles - 1 downTo 1) {
                val oldFile = File(logDirectory, "$baseFileName.$i.log")
                val newFile = File(logDirectory, "$baseFileName.${i + 1}.log")

                if (oldFile.exists()) {
                    if (i == maxFiles - 1) {
                        oldFile.delete() // Delete oldest file
                    } else {
                        oldFile.renameTo(newFile)
                    }
                }
            }

            // Move current file to .1
            currentFile?.renameTo(File(logDirectory, "$baseFileName.1.log"))

            // Create new current file
            initializeCurrentFile()
            currentFileSize = 0L

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getOrCreateWriter(): FileWriter {
        if (currentWriter == null) {
            currentWriter = FileWriter(currentFile, true)
        }
        return currentWriter!!
    }

    override suspend fun flush(): Unit = mutex.withLock {
        currentWriter?.flush()
    }

    override suspend fun close() = mutex.withLock {
        currentWriter?.close()
        currentWriter = null
    }

    override fun getName(): String = "File($baseFileName)"
}