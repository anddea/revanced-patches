package app.revanced.generator

import app.revanced.patcher.patch.loadPatchesFromJar
import java.io.File

internal fun main() = loadPatchesFromJar(
    setOf(File("build/libs/").listFiles { file ->
        val fileName = file.name
        !fileName.contains("javadoc") &&
                !fileName.contains("sources") &&
                fileName.endsWith(".rvp")
    }!!.first()),
).also { loader ->
    if (loader.isEmpty()) throw IllegalStateException("No patches found")
}.let { bundle ->
    arrayOf(
        JsonPatchesFileGenerator(),
        ReadMeFileGenerator()
    ).forEach { generator -> generator.generate(bundle) }
}
