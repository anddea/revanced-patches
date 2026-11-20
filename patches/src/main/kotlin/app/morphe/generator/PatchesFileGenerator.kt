package app.morphe.generator

import app.morphe.patcher.patch.Patch

/** Generates patch metadata using Patcher 2.3's unified [Patch] type. */
internal interface PatchesFileGenerator {
    fun generate(version: String, patches: Set<Patch>)
}
