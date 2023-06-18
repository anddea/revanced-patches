package app.revanced.patches.youtube.layout.fullscreen.seekmessage.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.fullscreen.seekmessage.fingerprints.SeekEduContainerFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.FULLSCREEN

@Patch
@Name("hide-seek-message")
@Description("Hides the 'Slide left or right to seek' message container.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SeekMessagePatch : BytecodePatch(
    listOf(SeekEduContainerFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        SeekEduContainerFingerprint.result?.mutableMethod?.let {
            it.addInstructionsWithLabels(
                0, """
                    invoke-static {}, $FULLSCREEN->hideSeekMessage()Z
                    move-result v0
                    if-eqz v0, :default
                    return-void
                """, ExternalLabel("default", it.getInstruction(0))
            )
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: HIDE_SEEK_MESSAGE"
            )
        )

        SettingsPatch.updatePatchStatus("hide-seek-message")

        return PatchResultSuccess()
    }
}
