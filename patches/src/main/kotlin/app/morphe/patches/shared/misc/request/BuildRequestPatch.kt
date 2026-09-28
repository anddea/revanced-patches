/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.shared.misc.request

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.findFreeRegister
import app.morphe.util.registersUsed
import java.lang.ref.WeakReference

private lateinit var buildRequestMethod: WeakReference<MutableMethod>
private var builderIndex = -1
private var urlRegister = -1
private var mapRegister = -1
private var freeRegister = -1

internal val buildRequestPatch = bytecodePatch(
    description = "buildRequestPatch",
) {
    execute {
        getBuildRequestFingerprint().let {
            it.method.apply {
                val builderMatch = it.instructionMatches.first()
                builderIndex = builderMatch.index
                urlRegister = builderMatch.instruction.registersUsed[1]
                mapRegister = it.instructionMatches[1].instruction.registersUsed[0]
                freeRegister = findFreeRegister(builderIndex, urlRegister, mapRegister)
                buildRequestMethod = WeakReference(this)
            }
        }
    }
}

/**
 * Hooks an InnerTube request after its URL and headers have been assembled.
 *
 * Header hooks replace the map so callers can atomically update related values such as
 * cold-config data and its matching hash.
 */
internal fun hookBuildRequest(
    descriptor: String,
    hookHeader: Boolean = false,
) {
    buildRequestMethod.get()!!.apply {
        if (hookHeader) {
            addInstructions(
                builderIndex,
                """
                    invoke-static { v$urlRegister, v$mapRegister }, $descriptor
                    move-result-object v$mapRegister
                """
            )
            builderIndex += 2
        } else {
            addInstructions(
                builderIndex++,
                "invoke-static { v$urlRegister, v$mapRegister }, $descriptor"
            )
        }
    }
}

internal fun hookBuildRequestUrl(descriptor: String) {
    buildRequestMethod.get()!!.addInstructions(
        builderIndex,
        """
            invoke-static { v$urlRegister }, $descriptor
            move-result-object v$urlRegister
        """
    )
    builderIndex += 2
}

internal fun hookBuildRequestBody(descriptor: String) {
    buildRequestMethod.get()!!.addInstructions(
        builderIndex,
        """
            move-object/from16 v$freeRegister, p2
            invoke-static { v$urlRegister, v$freeRegister }, $descriptor
            move-result-object v$freeRegister
            move-object/from16 p2, v$freeRegister
        """
    )
    builderIndex += 4
}
