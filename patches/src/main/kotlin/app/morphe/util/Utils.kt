package app.morphe.util

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

internal fun Boolean.toHexString(): String = if (this) "0x1" else "0x0"

internal val String.className: String
    get() = if (startsWith("L") && endsWith(";"))
        substring(1, length - 1).replace('/', '.')
    else replace('/', '.')
