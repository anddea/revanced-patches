package app.revanced.meta

import app.revanced.patcher.data.Context
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import app.revanced.patcher.extensions.PatchExtensions.description
import app.revanced.patcher.extensions.PatchExtensions.patchName
import app.revanced.patcher.patch.Patch
import com.unascribed.flexver.FlexVerComparator
import java.io.File

internal class ReadmeGenerator : PatchesFileGenerator {
    private companion object {
        private const val TABLE_HEADER =
            "| \uD83D\uDC8A Patch | \uD83D\uDCDC Description | \uD83C\uDFF9 Target Version |\n" +
                    "|:--------:|:--------------:|:-----------------:|"
    }

    override fun generate(bundle: PatchBundlePatches) {
        val output = StringBuilder()

        mutableMapOf<String, MutableList<Class<out Patch<Context<*>>>>>()
            .apply {
                for (patch in bundle) {
                    patch.compatiblePackages?.forEach { pkg ->
                        if (!contains(pkg.name)) put(pkg.name, mutableListOf())
                        this[pkg.name]!!.add(patch)
                    }
                }
            }
            .entries
            .sortedByDescending { it.value.size }
            .forEach { (`package`, patches) ->
                val supportVersions = buildMap {
                    patches.forEach { patch ->
                        patch.compatiblePackages?.single { compatiblePackage -> compatiblePackage.name == `package` }?.versions?.let {
                            it.forEach { version -> merge(version, 1, Integer::sum) }
                        }
                    }
                }

                val minVersion = supportVersions.let { commonMap ->
                    commonMap.maxByOrNull { it.value }?.value?.let {
                        commonMap.entries.filter { supported -> supported.value == it }
                            .minOfWith(FlexVerComparator::compare, Map.Entry<String, Int>::key)
                    } ?: "all"
                }
                val maxVersion = supportVersions.let { commonMap ->
                    commonMap.maxByOrNull { it.value }?.value?.let {
                        commonMap.entries.filter { supported -> supported.value == it }
                            .maxOfWith(FlexVerComparator::compare, Map.Entry<String, Int>::key)
                    } ?: "all"
                }

                output.apply {
                    appendLine("### [\uD83D\uDCE6 `${`package`}`](https://play.google.com/store/apps/details?id=${`package`})")
                    appendLine("<details>\n")
                    appendLine(TABLE_HEADER)
                    patches.forEach { patch ->
                        val supportedVersion =
                            if (
                                patch.compatiblePackages?.single { it.name == `package` }?.versions?.isNotEmpty() == true
                            ) {
                                if (minVersion == maxVersion)
                                    maxVersion
                                else
                                    "$minVersion ~ $maxVersion"
                            } else
                                "all"

                        appendLine(
                            "| `${patch.patchName.lowercase().replace(" ", "-")}` " +
                                    "| ${patch.description} " +
                                    "| $supportedVersion |"
                        )
                    }
                    appendLine("</details>\n")
                }
            }

        StringBuilder(File("README-template.md").readText())
            .replace(Regex("\\{\\{\\s?table\\s?}}"), output.toString())
            .let(File("README.md")::writeText)
    }
}