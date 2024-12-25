package app.revanced.generator

import app.revanced.patcher.patch.Patch
import com.google.gson.GsonBuilder
import java.io.File

typealias PackageName = String
typealias VersionName = String

internal class JsonPatchesFileGenerator : PatchesFileGenerator {
    override fun generate(patches: Set<Patch<*>>) {
        val patchesJson = File("../patches.json")
        patches.sortedBy { it.name }.map {
            JsonPatch(
                it.name!!,
                it.description,
                it.use,
                it.dependencies.map { dependency -> dependency.name ?: dependency.toString() },
                it.compatiblePackages?.associate { (packageName, versions) -> packageName to versions },
                it.options.values.map { option ->
                    JsonPatch.Option(
                        option.key,
                        option.title,
                        option.description,
                        option.required,
                        option.type.toString(),
                        option.default,
                        option.values,
                    )
                },
            )
        }.let {
            patchesJson.writeText(GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(it))
        }
    }

    @Suppress("unused")
    private class JsonPatch(
        val name: String? = null,
        val description: String? = null,
        val use: Boolean = true,
        val dependencies: List<String>,
        val compatiblePackages: Map<PackageName, Set<VersionName>?>? = null,
        val options: List<Option>,
    ) {
        class Option(
            val key: String,
            val title: String?,
            val description: String?,
            val required: Boolean,
            val type: String,
            val default: Any?,
            val values: Map<String, Any?>?,
        )
    }

}
