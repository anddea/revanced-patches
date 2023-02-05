package app.revanced.patches.youtube.button.autorepeat.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.button.autorepeat.fingerprints.*
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.toErrorResult
import app.revanced.shared.util.integrations.Constants.UTILS_PATH
import app.revanced.shared.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("always-autorepeat")
@YouTubeCompatibility
@Version("0.0.1")
class AutoRepeatPatch : BytecodePatch(
    listOf(
        AutoRepeatParentFingerprint,
        AutoNavInformerFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        AutoRepeatParentFingerprint.result?.classDef?.let { classDef ->
            AutoRepeatFingerprint.also {
                it.resolve(context, classDef)
            }.result?.mutableMethod?.let {
                it.addInstructions(
                    0, """
                    invoke-static {}, $VIDEO_PATH/VideoInformation;->videoEnded()Z
                    move-result v0
                    if-eqz v0, :noautorepeat
                    return-void
                """, listOf(ExternalLabel("noautorepeat", it.instruction(0)))
                )
            } ?: return AutoRepeatFingerprint.toErrorResult()
        } ?: return AutoRepeatParentFingerprint.toErrorResult()

        AutoNavInformerFingerprint.result?.mutableMethod?.let {
            with (it.implementation!!.instructions) {
                val index = this.size - 1 - 1
                val register = (this[index] as OneRegisterInstruction).registerA
                it.addInstructions(
                    index + 1, """
                    invoke-static {v$register}, $UTILS_PATH/EnableAutoRepeatPatch;->enableAutoRepeat(Z)Z
                    move-result v0
                """
                )

            }
        } ?: return AutoNavInformerFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
