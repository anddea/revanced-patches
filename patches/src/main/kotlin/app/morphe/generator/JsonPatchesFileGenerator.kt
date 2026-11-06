package app.morphe.generator

import app.morphe.patcher.patch.Patch
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.File

typealias PackageName = String
typealias VersionName = String

internal class JsonPatchesFileGenerator : PatchesFileGenerator {
    @Suppress("DEPRECATION")
    override fun generate(version: String, patches: Set<Patch<*>>) {
        val listJson = File("../patches-list.json")

        val patchesMap = patches.sortedBy { it.name }.map {
            JsonPatch(
                it.name!!,
                it.description,
                it.default,
                it.dependencies.map { dependency -> dependency.javaClass.simpleName },
                // Keep the published JSON schema while reading Patcher 2.0 compatibility metadata.
                it.compatibility?.associate { compatibility ->
                    requireNotNull(compatibility.packageName) to compatibility.targets
                        .mapNotNull { target -> target.version }.toSet().takeIf { it.isNotEmpty() }
                },
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
        }

        val gsonBuilder = GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create()

        val jsonObject = JsonObject()
        jsonObject.addProperty("version", "v$version")
        jsonObject.add("patches", gsonBuilder.toJsonTree(patchesMap))

        listJson.writeText(
            gsonBuilder.toJson(jsonObject)
        )
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
