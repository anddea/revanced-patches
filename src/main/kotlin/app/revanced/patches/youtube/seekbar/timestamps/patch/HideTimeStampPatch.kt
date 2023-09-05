package app.revanced.patches.youtube.seekbar.timestamps.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.seekbar.timestamps.fingerprints.TimeCounterFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.PlayerSeekbarColorFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.SEEKBAR

@Patch
@Name("Hide time stamp")
@Description("Hides timestamp in video player.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class,
    ]
)
@YouTubeCompatibility
class HideTimeStampPatch : BytecodePatch(
    listOf(PlayerSeekbarColorFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        PlayerSeekbarColorFingerprint.result?.let { parentResult ->
            TimeCounterFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                it.mutableMethod.apply {
                    addInstructionsWithLabels(
                        0, """
                        invoke-static {}, $SEEKBAR->hideTimeStamp()Z
                        move-result v0
                        if-eqz v0, :show_time_stamp
                        return-void
                        """, ExternalLabel("show_time_stamp", getInstruction(0))
                    )
                }
            } ?: throw TimeCounterFingerprint.exception
        } ?: throw PlayerSeekbarColorFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: HIDE_TIME_STAMP"
            )
        )

        SettingsPatch.updatePatchStatus("hide-time-stamp")

    }
}
