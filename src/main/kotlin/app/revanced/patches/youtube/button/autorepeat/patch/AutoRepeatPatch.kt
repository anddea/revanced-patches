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
import app.revanced.patches.youtube.button.autorepeat.fingerprints.AutoNavInformerFingerprint
import app.revanced.patches.youtube.button.autorepeat.fingerprints.AutoRepeatFingerprint
import app.revanced.patches.youtube.button.autorepeat.fingerprints.AutoRepeatParentFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.UTILS_PATH
import app.revanced.shared.util.integrations.Constants.VIDEO_PATH

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
        with(AutoRepeatFingerprint.also {
            it.resolve(context, AutoRepeatParentFingerprint.result!!.classDef)
        }.result!!.mutableMethod) {
            addInstructions(
                0, """
                    invoke-static {}, $VIDEO_PATH/VideoInformation;->videoEnded()Z
                    move-result v0
                    if-eqz v0, :noautorepeat
                    return-void
                """, listOf(ExternalLabel("noautorepeat", instruction(0)))
            )
        }

        with(AutoNavInformerFingerprint.result!!.mutableMethod) {
            addInstructions(
                0, """
                    invoke-static {}, $UTILS_PATH/EnableAutoRepeatPatch;->enableAutoRepeat()Z
                    move-result v0
                    if-eqz v0, :hidden
                    const/4 v0, 0x0
                    return v0
                """, listOf(ExternalLabel("hidden", instruction(0)))
            )
        }

        return PatchResultSuccess()
    }
}
