package app.revanced.patches.music.misc.sharebuttonhook.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.misc.sharebuttonhook.fingerprints.ConnectionTrackerFingerprint
import app.revanced.patches.music.misc.sharebuttonhook.fingerprints.FullStackTraceActivityFingerprint
import app.revanced.patches.music.misc.sharebuttonhook.fingerprints.SharePanelFingerprint
import app.revanced.patches.music.misc.sharebuttonhook.fingerprints.ShowToastFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.music.utils.videoid.patch.VideoIdPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_INTEGRATIONS_PATH
import app.revanced.util.integrations.Constants.MUSIC_MISC_PATH

@Patch
@Name("Share button hook")
@Description("Replace share button with external download button.")
@DependsOn(
    [
        SettingsPatch::class,
        VideoIdPatch::class
    ]
)
@MusicCompatibility
class ShareButtonHookPatch : BytecodePatch(
    listOf(
        ConnectionTrackerFingerprint,
        FullStackTraceActivityFingerprint,
        SharePanelFingerprint,
        ShowToastFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        SharePanelFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex

                addInstructionsWithLabels(
                    targetIndex, """
                        invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->shouldHookShareButton()Z
                        move-result p1
                        if-eqz p1, :default
                        invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->startDownloadActivity()V
                        return-void
                        """, ExternalLabel("default", getInstruction(targetIndex))
                )
            }
        } ?: throw SharePanelFingerprint.exception

        ConnectionTrackerFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "sput-object p1, $INTEGRATIONS_CLASS_DESCRIPTOR->context:Landroid/content/Context;"
        ) ?: throw ConnectionTrackerFingerprint.exception

        ShowToastFingerprint.result?.mutableMethod?.addInstructions(
            0, """
                invoke-static {p0}, $INTEGRATIONS_CLASS_DESCRIPTOR->dismissContext(Landroid/content/Context;)Landroid/content/Context;
                move-result-object p0
                """
        ) ?: throw ShowToastFingerprint.exception

        FullStackTraceActivityFingerprint.result?.mutableMethod?.addInstructions(
            1, """
                invoke-static {p0}, $MUSIC_INTEGRATIONS_PATH/settingsmenu/SharedPreferenceChangeListener;->initializeSettings(Landroid/app/Activity;)V
                return-void
                """
        ) ?: throw FullStackTraceActivityFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.MISC,
            "revanced_hook_share_button",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_external_downloader_package_name",
            "revanced_hook_share_button"
        )

    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR = "$MUSIC_MISC_PATH/HookShareButtonPatch;"
    }
}
