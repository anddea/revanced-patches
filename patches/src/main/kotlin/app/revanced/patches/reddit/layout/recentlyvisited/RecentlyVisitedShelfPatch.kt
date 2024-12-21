package app.revanced.patches.reddit.layout.recentlyvisited

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.patch.PatchList.HIDE_RECENTLY_VISITED_SHELF
import app.revanced.patches.reddit.utils.settings.settingsPatch
import app.revanced.patches.reddit.utils.settings.updatePatchStatus
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

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

        val recentlyVisitedReference =
            with(communityDrawerPresenterConstructorFingerprint.methodOrThrow()) {
                val recentlyVisitedFieldIndex = indexOfHeaderItemInstruction(this)
                val recentlyVisitedObjectIndex =
                    indexOfFirstInstructionOrThrow(recentlyVisitedFieldIndex, Opcode.IPUT_OBJECT)

                getInstruction<ReferenceInstruction>(recentlyVisitedObjectIndex).reference.toString()
            }

        communityDrawerPresenterFingerprint.methodOrThrow(
            communityDrawerPresenterConstructorFingerprint
        ).apply {
            val recentlyVisitedObjectIndex =
                indexOfFirstInstructionOrThrow {
                    (this as? ReferenceInstruction)?.reference?.toString() == recentlyVisitedReference
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
