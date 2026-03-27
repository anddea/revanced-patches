package app.morphe.patches.reddit.layout.recentlyvisited

import app.morphe.patches.reddit.utils.extension.Constants
import app.morphe.patches.reddit.utils.patch.PatchList
import app.morphe.patches.reddit.utils.settings.settingsPatch
import app.morphe.patches.reddit.utils.settings.updatePatchStatus
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val EXTENSION_METHOD_DESCRIPTOR =
    "${Constants.PATCHES_PATH}/RecentlyVisitedShelfPatch;" +
            "->" +
            "hideRecentlyVisitedShelf(Ljava/util/List;)Ljava/util/List;"

@Suppress("unused")
val recentlyVisitedShelfPatch = bytecodePatch(
    PatchList.HIDE_RECENTLY_VISITED_SHELF.title,
    PatchList.HIDE_RECENTLY_VISITED_SHELF.summary,
) {
    compatibleWith(app.morphe.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE)

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
            PatchList.HIDE_RECENTLY_VISITED_SHELF
        )
    }
}
