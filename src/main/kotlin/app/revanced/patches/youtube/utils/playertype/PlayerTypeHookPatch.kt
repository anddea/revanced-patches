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
import app.revanced.patches.youtube.utils.playertype.fingerprint.VideoStateFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.addFieldAndInstructions
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Patch(dependencies = [SharedResourceIdPatch::class])
object PlayerTypeHookPatch : BytecodePatch(
    setOf(
        ActionBarSearchResultsFingerprint,
        BrowseIdClassFingerprint,
        PlayerTypeFingerprint,
        YouTubeControlsOverlayFingerprint
    )
) {
    private const val INTEGRATIONS_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR =
        "$UTILS_PATH/PlayerTypeHookPatch;"

    private const val INTEGRATIONS_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR =
        "$SHARED_PATH/RootView;"

    override fun execute(context: BytecodeContext) {

        // region patch for set player type

        PlayerTypeFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstruction(
                    0,
                    "invoke-static {p1}, " +
                            "$INTEGRATIONS_PLAYER_TYPE_HOOK_CLASS_DESCRIPTOR->setPlayerType(Ljava/lang/Enum;)V"
                )
            }
        }

        // endregion

        // region patch for set video state

        YouTubeControlsOverlayFingerprint.resultOrThrow().let { parentResult ->
            VideoStateFingerprint.also { it.resolve(context, parentResult.classDef)
            }.resultOrThrow().let {
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
        }

        // endregion

        // region patch for hook browse id

        BrowseIdClassFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getStringInstructionIndex("VL") - 1
                val targetReference = getInstruction<ReferenceInstruction>(targetIndex).reference
                val targetClass = context.findClass((targetReference as FieldReference).definingClass)!!.mutableClass

                targetClass.methods.find { method -> method.name == "<init>" }
                    ?.apply {
                        val browseIdFieldIndex = getTargetIndex(Opcode.IPUT_OBJECT)
                        val browseIdFieldName =
                            (getInstruction<ReferenceInstruction>(browseIdFieldIndex).reference as FieldReference).name

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

                        val rootViewMutableClass =
                            context.findClass(INTEGRATIONS_ROOT_VIEW_HOOK_CLASS_DESCRIPTOR)!!.mutableClass

                        rootViewMutableClass.addFieldAndInstructions(
                            context,
                            "getBrowseId",
                            "browseIdClass",
                            definingClass,
                            smaliInstructions,
                            true
                        )
                    } ?: throw PatchException("BrowseIdClass not found!")
            }
        }

        // endregion

        // region patch for hook search bar

        // Two different layouts are used at the hooked code.
        // Insert before the first ViewGroup method call after inflating,
        // so this works regardless which layout is used.
        ActionBarSearchResultsFingerprint.resultOrThrow().mutableMethod.apply {
            val instructionIndex = getTargetIndexWithMethodReferenceName("setLayoutDirection")
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
