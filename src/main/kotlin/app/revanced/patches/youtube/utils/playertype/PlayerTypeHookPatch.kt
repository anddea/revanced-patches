package app.revanced.patches.youtube.utils.playertype

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.SHARED_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.playertype.fingerprint.ActionBarSearchResultsFingerprint
import app.revanced.patches.youtube.utils.playertype.fingerprint.BrowseIdClassFingerprint
import app.revanced.patches.youtube.utils.playertype.fingerprint.PlayerTypeFingerprint
import app.revanced.patches.youtube.utils.playertype.fingerprint.ReelWatchPagerFingerprint
import app.revanced.patches.youtube.utils.playertype.fingerprint.VideoStateFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelWatchPlayer
import app.revanced.util.addStaticFieldToIntegration
import app.revanced.util.alsoResolve
import app.revanced.util.findMethodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.indexOfFirstWideLiteralInstructionValueOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Patch(dependencies = [SharedResourceIdPatch::class])
object PlayerTypeHookPatch : BytecodePatch(
    setOf(
        ActionBarSearchResultsFingerprint,
        BrowseIdClassFingerprint,
        PlayerTypeFingerprint,
        ReelWatchPagerFingerprint,
        YouTubeControlsOverlayFingerprint
    )
) {
    private const val INTEGRATIONS_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR =
        "$UTILS_PATH/PlayerTypeHookPatch;"

    private const val INTEGRATIONS_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR =
        "$SHARED_PATH/RootView;"

    override fun execute(context: BytecodeContext) {

        // region patch for set player type

        PlayerTypeFingerprint.resultOrThrow().mutableMethod.addInstruction(
            0,
            "invoke-static {p1}, " +
                    "$INTEGRATIONS_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR->setPlayerType(Ljava/lang/Enum;)V"
        )

        // endregion

        // region patch for set shorts player state

        ReelWatchPagerFingerprint.resultOrThrow().mutableMethod.apply {
            val literIndex = indexOfFirstWideLiteralInstructionValueOrThrow(ReelWatchPlayer) + 2
            val registerIndex = indexOfFirstInstructionOrThrow(literIndex) {
                opcode == Opcode.MOVE_RESULT_OBJECT
            }
            val viewRegister = getInstruction<OneRegisterInstruction>(registerIndex).registerA

            addInstruction(
                registerIndex + 1,
                "invoke-static {v$viewRegister}, " +
                        "$INTEGRATIONS_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR->onShortsCreate(Landroid/view/View;)V"
            )
        }

        // endregion

        // region patch for set video state

        VideoStateFingerprint.alsoResolve(
            context, YouTubeControlsOverlayFingerprint
        ).let {
            it.mutableMethod.apply {
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val videoStateFieldName =
                    getInstruction<ReferenceInstruction>(endIndex).reference

                addInstructions(
                    0, """
                        iget-object v0, p1, $videoStateFieldName  # copy VideoState parameter field
                        invoke-static {v0}, $INTEGRATIONS_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR->setVideoState(Ljava/lang/Enum;)V
                        """
                )
            }
        }

        // endregion

        // region patch for hook browse id

        BrowseIdClassFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = indexOfFirstStringInstructionOrThrow("VL") - 1
                val targetClass = getInstruction(targetIndex)
                    .getReference<FieldReference>()
                    ?.definingClass
                    ?: throw PatchException("Could not find browseId class")

                context.findMethodOrThrow(targetClass).apply {
                    val browseIdFieldReference = getInstruction<ReferenceInstruction>(
                        indexOfFirstInstructionOrThrow(Opcode.IPUT_OBJECT)
                    ).reference
                    val browseIdFieldName = (browseIdFieldReference as FieldReference).name

                    val smaliInstructions =
                        """
                            if-eqz v0, :ignore
                            iget-object v0, v0, $definingClass->$browseIdFieldName:Ljava/lang/String;
                            if-eqz v0, :ignore
                            return-object v0
                            :ignore
                            const-string v0, ""
                            return-object v0
                            """

                    context.addStaticFieldToIntegration(
                        INTEGRATIONS_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR,
                        "getBrowseId",
                        "browseIdClass",
                        definingClass,
                        smaliInstructions
                    )
                }
            }
        }

        // endregion

        // region patch for hook search bar

        // Two different layouts are used at the hooked code.
        // Insert before the first ViewGroup method call after inflating,
        // so this works regardless which layout is used.
        ActionBarSearchResultsFingerprint.resultOrThrow().mutableMethod.apply {
            val instructionIndex =
                ActionBarSearchResultsFingerprint.indexOfLayoutDirectionInstruction(this)
            val viewRegister = getInstruction<FiveRegisterInstruction>(instructionIndex).registerC

            addInstruction(
                instructionIndex,
                "invoke-static { v$viewRegister }, " +
                        "$INTEGRATIONS_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR->searchBarResultsViewLoaded(Landroid/view/View;)V",
            )
        }

        // endregion

    }
}
