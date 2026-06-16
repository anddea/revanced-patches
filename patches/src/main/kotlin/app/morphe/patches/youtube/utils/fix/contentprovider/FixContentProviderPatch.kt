package app.morphe.patches.youtube.utils.fix.contentprovider

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.extension.Constants.PATCHES_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS =
    "$PATCHES_PATH/FixContentProviderPatch;"

private const val ADD_NULL_MAP_ENTRY_METHOD =
    "$EXTENSION_CLASS->addNullMapEntry(Ljava/util/Map;)V"

/**
 * Injects a null content provider map value, so the ConcurrentMap crash can be reproduced reliably.
 */
internal val contentProviderNullMapEntryReproPatch = bytecodePatch(
    description = "contentProviderNullMapEntryReproPatch",
) {
    dependsOn(
        sharedExtensionPatch
    )

    execute {
        UnstableContentProviderFingerprint.let {
            val insertIndex = it.instructionMatches.first().index

            it.method.apply {
                val register = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstruction(
                    insertIndex,
                    "invoke-static { v$register }, $EXTENSION_CLASS->addNullMapEntry(Ljava/util/Map;)V"
                )
            }
        }
    }
}

/**
 * Fixes crashing for some users with a beta release where the YouTube content provider uses null map values.
 * It unknown if this crash can happen on stable releases.
 */
internal val fixContentProviderPatch = bytecodePatch(
    description = "fixContentProviderPatch",
) {
    dependsOn(
        sharedExtensionPatch,
        contentProviderNullMapEntryReproPatch,
    )

    execute {
        UnstableContentProviderFingerprint.let {
            it.method.apply {
                val reproIndex = indexOfFirstInstruction {
                    opcode == Opcode.INVOKE_STATIC &&
                            getReference<MethodReference>()?.toString() == ADD_NULL_MAP_ENTRY_METHOD
                }
                val insertIndex: Int
                val register: Int

                if (reproIndex >= 0) {
                    insertIndex = reproIndex + 1
                    register = getInstruction<FiveRegisterInstruction>(reproIndex).registerC
                } else {
                    insertIndex = it.instructionMatches.first().index
                    register = getInstruction<FiveRegisterInstruction>(insertIndex).registerD
                }

                addInstruction(
                    insertIndex,
                    "invoke-static { v$register }, $EXTENSION_CLASS->removeNullMapEntries(Ljava/util/Map;)V"
                )
            }
        }
    }
}
