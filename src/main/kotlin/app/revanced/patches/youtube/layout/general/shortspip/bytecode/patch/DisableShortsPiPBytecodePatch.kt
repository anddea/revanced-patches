package app.revanced.patches.youtube.layout.general.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.layout.general.bytecode.fingerprints.DisableShortsPiPFingerprint
import app.revanced.patches.youtube.misc.playertype.patch.PlayerTypeHookPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.toErrorResult
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("disable-shorts-pip-bytecode-patch")
@DependsOn(
    [
        PlayerTypeHookPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class DisableShortsPiPBytecodePatch : BytecodePatch(
    listOf(
        DisableShortsPiPFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        DisableShortsPiPFingerprint.result?.mutableMethod?.let { method ->

            with (method.implementation!!) {
                val invokeIndex = this.instructions.indexOfFirst {
                    it.opcode.ordinal == Opcode.INVOKE_VIRTUAL.ordinal &&
                            ((it as? BuilderInstruction35c)?.reference.toString() ==
                                    "$REFERENCE_DESCRIPTOR")
                }
                val registerA = (method.instruction(invokeIndex + 1) as OneRegisterInstruction).registerA

                val registerC = (method.instruction(invokeIndex) as BuilderInstruction35c).registerC
                val registerE = (method.instruction(invokeIndex) as BuilderInstruction35c).registerE

                method.addInstructions(
                    invokeIndex + 1,"""
                        invoke-static {}, $GENERAL_LAYOUT->disableShortsPlayerPiP()Z
                        move-result v$registerA
                        if-eqz v$registerA, :pip
                        goto :disablepip
                        :pip
                        invoke-virtual {v${registerC}, v${registerE}}, $REFERENCE_DESCRIPTOR
                        move-result v$registerA
                    """, listOf(ExternalLabel("disablepip", method.instruction(invokeIndex + 1)))
                )
                method.removeInstruction(invokeIndex)
                method.replaceInstruction(
                    invokeIndex + 6, "nop"
                )

            }
        } ?: return DisableShortsPiPFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    private companion object {
        const val REFERENCE_DESCRIPTOR =
            "Landroid/app/Activity;->enterPictureInPictureMode(Landroid/app/PictureInPictureParams;)Z"
    }
}