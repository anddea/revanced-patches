package app.revanced.util

import java.util.logging.Logger

internal object Utils {
    internal fun String.trimIndentMultiline() =
        this.split("\n")
            .joinToString("\n") { it.trimIndent() } // Remove the leading whitespace from each line.
            .trimIndent() // Remove the leading newline.

    private val logger = Logger.getLogger(this::class.java.name)

    internal fun printInfo(msg: String) =
        logger.info(msg)

    internal fun printWarn(msg: String) =
        logger.warning(msg)
}
