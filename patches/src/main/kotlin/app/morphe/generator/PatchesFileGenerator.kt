package app.morphe.generator

import app.morphe.patcher.patch.Patch

internal interface PatchesFileGenerator {
    fun generate(version: String, patches: Set<Patch<*>>)
}
