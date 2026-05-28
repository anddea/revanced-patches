package app.morphe.generator

import app.morphe.patcher.patch.Patch
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths

internal class ReadMeFileGenerator : PatchesFileGenerator {
    // For this exception to apply to [README.md],
    // supported versions should be empty.
    private val exception = mapOf(
        "com.google.android.apps.youtube.music" to "6.29.59"
    )

    private val tableHeader =
        "| \uD83D\uDC8A Patch | \uD83D\uDCDC Description | \uD83C\uDFF9 Target Version |\n" +
                "|:--------:|:--------------:|:-----------------:|"

    override fun generate(version: String, patches: Set<Patch<*>>) {
        val rootPath = Paths.get("").toAbsolutePath().parent!!
        val readMeFilePath = "$rootPath/README.md"

        val readMeFile = File(readMeFilePath)
        val readMeTemplateFile = File("$rootPath/README-template.md")

        val output = StringBuilder()

        if (readMeFile.exists()) {
            PrintWriter(readMeFile).also {
                it.print("")
                it.close()
            }
        } else {
            Files.createFile(Paths.get(readMeFilePath))
        }

        // copy the contents of 'README-template.md' to the temp file
        StringBuilder(readMeTemplateFile.readText())
            .toString()
            .let(readMeFile::writeText)

        // add a list of supported versions to a temp file
        mapOf(
            app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC to "\"COMPATIBLE_PACKAGE_MUSIC\"",
            app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE to "\"COMPATIBLE_PACKAGE_YOUTUBE\""
        ).forEach { (compatibility, replaceString) ->
            val packageName = compatibility.packageName ?: return@forEach
            val versions = compatibility.targets.mapNotNullTo(linkedSetOf()) { it.version }
            val supportedVersion =
                if (versions.isEmpty() && exception.containsKey(packageName)) {
                    exception[packageName] + "+"
                } else if (versions.isNotEmpty()) {
                    versions
                        .toString()
                        .replace("[", "[\n        \"")
                        .replace("]", "\"\n      ]")
                        .replace(", ", "\",\n        \"")
                } else {
                    "\"ALL\""
                }

            StringBuilder(readMeFile.readText())
                .replace(Regex(replaceString), supportedVersion)
                .let(readMeFile::writeText)

            mutableMapOf<String, MutableSet<Patch<*>>>()
                .apply {
                    for (patch in patches) {
                        patch.compatibility?.mapNotNull { it.packageName }?.forEach { packageName ->
                            if (!contains(packageName)) put(packageName, mutableSetOf())
                            this[packageName]!!.add(patch)
                        }
                    }
                }
                .entries
                .sortedByDescending { it.value.size }
                .forEach { (pkg, patches) ->
                    output.apply {
                        appendLine("### [\uD83D\uDCE6 `$pkg`](https://play.google.com/store/apps/details?id=$pkg)")
                        appendLine("<details>\n")
                        appendLine(tableHeader)
                        patches.sortedBy { it.name }.forEach { patch ->
                            val supportedVersionArray =
                                patch.compatibility?.lastOrNull()?.targets?.mapNotNull { it.version }
                            val supportedVersion =
                                if (!supportedVersionArray.isNullOrEmpty()) {
                                    val minVersion = supportedVersionArray.first()
                                    val maxVersion = supportedVersionArray.last()
                                    if (minVersion == maxVersion)
                                        maxVersion
                                    else
                                        "$minVersion ~ $maxVersion"
                                } else if (exception.containsKey(pkg))
                                    exception[pkg] + "+"
                                else
                                    "ALL"

                            appendLine(
                                "| `${patch.name}` " +
                                        "| ${patch.description} " +
                                        "| $supportedVersion |"
                            )
                        }
                        appendLine("</details>\n")
                    }
                }

            // copy the contents of the temp file to 'README.md'
            StringBuilder(readMeFile.readText())
                .replace(Regex("\\{\\{\\s?table\\s?}}"), output.toString())
                .let(readMeFile::writeText)
        }
    }
}
