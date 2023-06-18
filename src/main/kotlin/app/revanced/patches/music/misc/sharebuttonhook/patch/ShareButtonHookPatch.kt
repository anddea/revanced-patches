package app.revanced.patches.music.misc.sharebuttonhook.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.misc.settings.resource.patch.MusicSettingsPatch
import app.revanced.patches.music.misc.sharebuttonhook.fingerprints.*
import app.revanced.patches.music.misc.sleeptimerhook.patch.SleepTimerHookPatch
import app.revanced.patches.music.misc.videoid.patch.MusicVideoIdPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_INTEGRATIONS_PATH
import app.revanced.util.integrations.Constants.MUSIC_MISC_PATH

@Patch
@Name("share-button-hook")
@Description("Replace share button with external download button or sleep timer dialog.")
@DependsOn(
    [
        MusicSettingsPatch::class,
        MusicVideoIdPatch::class,
        SleepTimerHookPatch::class
    ]
)
@YouTubeMusicCompatibility
@Version("0.0.1")
class ShareButtonHookPatch : BytecodePatch(
    listOf(
        ConnectionTrackerFingerprint,
        FullStackTraceActivityFingerprint,
        SharePanelFingerprint,
        ShowToastFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        SharePanelFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex

                addInstructionsWithLabels(
                    targetIndex,"""
                        invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->overrideSharePanel()Z
                        move-result p1
                        if-eqz p1, :default
                        return-void
                        """, ExternalLabel("default", getInstruction(targetIndex))
                )
            }
        } ?: return SharePanelFingerprint.toErrorResult()

        ConnectionTrackerFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "sput-object p1, $INTEGRATIONS_CLASS_DESCRIPTOR->context:Landroid/content/Context;"
        ) ?: return ConnectionTrackerFingerprint.toErrorResult()

        ShowToastFingerprint.result?.mutableMethod?.addInstructions(
            0,"""
                invoke-static {p0}, $INTEGRATIONS_CLASS_DESCRIPTOR->dismissContext(Landroid/content/Context;)Landroid/content/Context;
                move-result-object p0
            """
        ) ?: return ShowToastFingerprint.toErrorResult()

        FullStackTraceActivityFingerprint.result?.mutableMethod?.addInstructions(
            1,"""
                invoke-static {p0}, $MUSIC_INTEGRATIONS_PATH/settingsmenu/SharedPreferenceChangeListener;->initializeSettings(Landroid/app/Activity;)V
                return-void
                """
        ) ?: return FullStackTraceActivityFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference(CategoryType.MISC, "revanced_hook_share_button", "false")
        MusicSettingsPatch.addMusicPreferenceAlt(CategoryType.MISC, "revanced_hook_type", "false", "revanced_hook_share_button")
        MusicSettingsPatch.addMusicPreferenceWithIntent(CategoryType.MISC, "revanced_default_downloader", "revanced_hook_share_button")

        return PatchResultSuccess()
    }
    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR = "$MUSIC_MISC_PATH/HookShareButtonPatch;"
    }
}
