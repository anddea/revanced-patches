package app.revanced.patches.reddit.layout.recentlyvisited

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.patch.PatchList.HIDE_RECENTLY_VISITED_SHELF
import app.revanced.patches.reddit.utils.settings.settingsPatch
import app.revanced.patches.reddit.utils.settings.updatePatchStatus
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$PATCHES_PATH/RecentlyVisitedShelfPatch;" +
            "->" +
            "hideRecentlyVisitedShelf(Ljava/util/List;)Ljava/util/List;"

@Suppress("unused")
val recentlyVisitedShelfPatch = bytecodePatch(
    HIDE_RECENTLY_VISITED_SHELF.title,
    HIDE_RECENTLY_VISITED_SHELF.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        val communityDrawerPresenterMethod = communityDrawerPresenterFingerprint.methodOrThrow()
        val constructorMethod = findMethodOrThrow(communityDrawerPresenterMethod.definingClass)
        val recentlyVisitedReference = with(constructorMethod) {
            val recentlyVisitedFieldIndex = indexOfFirstInstructionOrThrow {
                getReference<FieldReference>()?.name == "RECENTLY_VISITED"
            }
            val recentlyVisitedObjectIndex =
                indexOfFirstInstructionOrThrow(
                    recentlyVisitedFieldIndex,
                    Opcode.IPUT_OBJECT
                )
            getInstruction<ReferenceInstruction>(recentlyVisitedObjectIndex).reference
        }
        communityDrawerPresenterMethod.apply {
            val recentlyVisitedObjectIndex = indexOfFirstInstructionOrThrow {
                getReference<FieldReference>()?.toString() == recentlyVisitedReference.toString()
            }
            arrayOf(
                indexOfFirstInstructionOrThrow(
                    recentlyVisitedObjectIndex,
                    Opcode.INVOKE_STATIC
                ),
                indexOfFirstInstructionReversedOrThrow(
                    recentlyVisitedObjectIndex,
                    Opcode.INVOKE_STATIC
                )
            ).forEach { staticIndex ->
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(staticIndex + 1).registerA

                addInstructions(
                    staticIndex + 2, """
                            invoke-static {v$insertRegister}, $EXTENSION_METHOD_DESCRIPTOR
                            move-result-object v$insertRegister
                            """
                )
            }
        }

        updatePatchStatus(
            "enableRecentlyVisitedShelf",
            HIDE_RECENTLY_VISITED_SHELF
        )
    }
}
