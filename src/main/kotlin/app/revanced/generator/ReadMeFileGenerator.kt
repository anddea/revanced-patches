package app.revanced.generator

import app.revanced.patcher.PatchSet
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.Patch.CompatiblePackage
import java.io.File
import java.nio.file.Paths

internal class ReadMeFileGenerator : PatchesFileGenerator {
    // For this exception to apply to [README.md],
    // Supported version of [app.revanced.patches.music.utils.integrations.Constants.COMPATIBLE_PACKAGE] should be empty.
    private val exception = mapOf(
        "com.google.android.apps.youtube.music" to "6.29.58"
    )

    private val readMeFile = File("README.md")
    private val readMeTemplateFile = File("README-template.md")

    private val tableHeader =
        "| \uD83D\uDC8A Patch | \uD83D\uDCDC Description | \uD83C\uDFF9 Target Version |\n" +
                "|:--------:|:--------------:|:-----------------:|"

    override fun generate(patches: PatchSet) {
        val output = StringBuilder()

        // create a temp file
        val readMeTemplateTempFile = File.createTempFile("README", ".md", File(Paths.get("").toAbsolutePath().toString()))

        // copy the contents of 'README-template.md' to the temp file
        StringBuilder(readMeTemplateFile.readText())
            .toString()
            .let(readMeTemplateTempFile::writeText)

        // add a list of supported versions to a temp file
        mapOf(
            app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE to "\"COMPATIBLE_PACKAGE_MUSIC\"",
            app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE to "\"COMPATIBLE_PACKAGE_REDDIT\"",
            app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE to "\"COMPATIBLE_PACKAGE_YOUTUBE\""
        ).forEach { (compatiblePackage, replaceString) ->
            compatiblePackage.map { CompatiblePackage(it.name, it.versions?.toSet()?.ifEmpty { null }) }
                .forEach { compatiblePackages ->
                    val pkgName = compatiblePackages.name
                    val supportedVersion = if (compatiblePackages.versions == null && exception.containsKey(pkgName)) {
                        exception[pkgName] + "+"
                    } else {
                        compatiblePackages.versions
                            ?.toString()
                            ?.replace("[", "[\n          \"")
                            ?.replace("]", "\"\n        ]")
                            ?.replace(", ", "\",\n          \"")
                            ?: "\"ALL\""
                    }

                    StringBuilder(readMeTemplateTempFile.readText())
                        .replace(Regex(replaceString), supportedVersion)
                        .let(readMeTemplateTempFile::writeText)
                }
        }

        mutableMapOf<String, MutableSet<Patch<*>>>()
            .apply {
                for (patch in patches) {
                    patch.compatiblePackages?.forEach { pkg ->
                        if (!contains(pkg.name)) put(pkg.name, mutableSetOf())
                        this[pkg.name]!!.add(patch)
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
                            patch.compatiblePackages?.single { it.name == pkg }?.versions
                        val supportedVersion =
                            if (supportedVersionArray?.isNotEmpty() == true) {
                                val minVersion = supportedVersionArray.elementAt(0)
                                val maxVersion =
                                    supportedVersionArray.elementAt(supportedVersionArray.size - 1)
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
        StringBuilder(readMeTemplateTempFile.readText())
            .replace(Regex("\\{\\{\\s?table\\s?}}"), output.toString())
            .let(readMeFile::writeText)

        // delete temp file
        readMeTemplateTempFile.delete()
    }
}