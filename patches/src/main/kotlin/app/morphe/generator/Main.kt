package app.morphe.generator

import app.morphe.patcher.patch.loadPatchesFromJar
import java.io.File
import java.net.URLClassLoader
import java.util.jar.Manifest

internal fun main() =
    setOf(File("build/libs/").listFiles { file ->
        val fileName = file.name
        !fileName.contains("javadoc") &&
                !fileName.contains("sources") &&
                fileName.endsWith(".mpp")
    }!!.first(),
).also { loader ->
        if (loader.isEmpty()) throw IllegalStateException("No patches found")
}.let { patchFiles ->
        val loadedPatches = loadPatchesFromJar(patchFiles)
        val patchClassLoader = URLClassLoader(patchFiles.map { it.toURI().toURL() }.toTypedArray())
        val manifest = patchClassLoader.getResources("META-INF/MANIFEST.MF")

        while (manifest.hasMoreElements()) {
            Manifest(manifest.nextElement().openStream())
                .mainAttributes
                .getValue("Version")
                ?.let {
                    arrayOf(
                        JsonPatchesFileGenerator(),
                        ReadMeFileGenerator()
                    ).forEach { generator -> generator.generate(it, loadedPatches) }
                }
        }
}
