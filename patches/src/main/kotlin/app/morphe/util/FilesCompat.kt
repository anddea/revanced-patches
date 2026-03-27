package app.morphe.util

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Provides java.nio.file.Files compatible functions.
 *
 * This is needed for the ReVanced Manager running on Android 5.0-7.1
 * because Android 7.1 and below does not support the Java NIO2 Files API.
 */
internal object FilesCompat {
    private val useCompat = try {
        // Check for the existence of java.nio.file.Files class
        Class.forName("java.nio.file.Files")
        false
    } catch (_: ClassNotFoundException) {
        // Under Android 8.0
        true
    }

    /**
     * Copy a file to a target file.
     *
     * If the `target` file already exists, replace an existing file.
     */
    fun copy(source: File, target: File) {
        if (useCompat) {
            source.copyTo(target, overwrite = true)
        } else {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /**
     * Copies all bytes from an input stream to a file.
     *
     * If the `target` file already exists, replace an existing file.
     */
    fun copy(source: InputStream, target: File) {
        if (useCompat) {
            source.use { inputStream ->
                target.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } else {
            Files.copy(source, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}