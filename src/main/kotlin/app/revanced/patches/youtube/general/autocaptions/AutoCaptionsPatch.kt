package app.revanced.patches.youtube.general.autocaptions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.fingerprints.captions.SubtitleTrackFingerprint
import app.revanced.patches.youtube.general.autocaptions.fingerprints.StartVideoInformerFingerprint
import app.revanced.patches.youtube.utils.fingerprints.SubtitleButtonControllerFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception

@Patch(
    name = "Disable auto captions",
    description = "Disables forced auto captions.",
    dependencies = [
        PlayerTypeHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.43"
            ]
        )
    ]
)
@Suppress("unused")
object AutoCaptionsPatch : BytecodePatch(
    setOf(
        StartVideoInformerFingerprint,
        SubtitleButtonControllerFingerprint,
        SubtitleTrackFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        listOf(
            StartVideoInformerFingerprint.toPatch(Status.DISABLED),
            SubtitleButtonControllerFingerprint.toPatch(Status.ENABLED)
        ).forEach { (fingerprint, status) ->
            fingerprint.result?.mutableMethod?.addInstructions(
                0, """
                    const/4 v0, ${status.value}
                    sput-boolean v0, $GENERAL->captionsButtonStatus:Z
                    """
            ) ?: throw fingerprint.exception
        }

        SubtitleTrackFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $GENERAL->disableAutoCaptions()Z
                        move-result v0
                        if-eqz v0, :auto_captions_shown
                        sget-boolean v0, $GENERAL->captionsButtonStatus:Z
                        if-nez v0, :auto_captions_shown
                        const/4 v0, 0x1
                        return v0
                        """, ExternalLabel("auto_captions_shown", getInstruction(0))
                )
            }
        } ?: throw SubtitleTrackFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: DISABLE_AUTO_CAPTIONS"
            )
        )

        SettingsPatch.updatePatchStatus("Disable auto captions")

    }

    private fun MethodFingerprint.toPatch(visibility: Status) = SetStatus(this, visibility)

    private data class SetStatus(val fingerprint: MethodFingerprint, val visibility: Status)

    private enum class Status(val value: Int) {
        ENABLED(1),
        DISABLED(0)
    }
}