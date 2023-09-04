package app.revanced.patches.youtube.utils.returnyoutubedislike.oldlayout.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.DislikeButton
import app.revanced.patches.youtube.utils.returnyoutubedislike.oldlayout.fingerprints.ButtonTagFingerprint
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@DependsOn([SharedResourceIdPatch::class])
class ReturnYouTubeDislikeOldLayoutPatch : BytecodePatch(
    listOf(ButtonTagFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        ButtonTagFingerprint.result?.let {
            it.mutableMethod.apply {
                val dislikeButtonIndex = getWideLiteralIndex(DislikeButton)

                val resourceIdentifierRegister =
                    getInstruction<OneRegisterInstruction>(dislikeButtonIndex).registerA
                val textViewRegister =
                    getInstruction<OneRegisterInstruction>(dislikeButtonIndex + 4).registerA

                addInstruction(
                    dislikeButtonIndex + 4,
                    "invoke-static {v$resourceIdentifierRegister, v$textViewRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->setOldUILayoutDislikes(ILandroid/widget/TextView;)V"
                )
            }
        } ?: throw ButtonTagFingerprint.exception

    }

    private companion object {
        const val INTEGRATIONS_RYD_CLASS_DESCRIPTOR =
            "$UTILS_PATH/ReturnYouTubeDislikePatch;"
    }
}
