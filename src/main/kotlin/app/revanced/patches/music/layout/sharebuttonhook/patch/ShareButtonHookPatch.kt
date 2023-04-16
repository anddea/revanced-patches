package app.revanced.patches.music.layout.sharebuttonhook.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.layout.sharebuttonhook.fingerprints.*
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.patches.music.misc.videoid.patch.MusicVideoIdPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.patch.options.PatchOptions
import app.revanced.patches.shared.patch.options.PatchOptions.Companion.MusicDownloaderPackageName
import app.revanced.util.integrations.Constants.MUSIC_PATH

@Patch
@Name("share-button-hook")
@Description("Replace share button with external download button.")
@DependsOn(
    [
        MusicSettingsPatch::class,
        MusicVideoIdPatch::class,
        PatchOptions::class
    ]
)
@YouTubeMusicCompatibility
@Version("0.0.1")
class ShareButtonHookPatch : BytecodePatch(
    listOf(
        ConnectionTrackerFingerprint,
        MusicSettingsFingerprint,
        SharePanelFingerprint,
        ShowToastFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        SharePanelFingerprint.result?.let {
            with (it.mutableMethod) {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex

                addInstructions(
                    targetIndex,"""
                        invoke-static {}, $MUSIC_PATH/HookShareButtonPatch;->overrideSharePanel()Z
                        move-result p1
                        if-eqz p1, :default
                        return-void
                    """, listOf(ExternalLabel("default", instruction(targetIndex)))
                )
            }
        } ?: return SharePanelFingerprint.toErrorResult()

        ConnectionTrackerFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "sput-object p1, $MUSIC_PATH/HookShareButtonPatch;->context:Landroid/content/Context;"
        ) ?: return ConnectionTrackerFingerprint.toErrorResult()

        ShowToastFingerprint.result?.mutableMethod?.addInstructions(
            0,"""
                invoke-static {p0}, $MUSIC_PATH/HookShareButtonPatch;->dismissContext(Landroid/content/Context;)Landroid/content/Context;
                move-result-object p0
            """
        ) ?: return ShowToastFingerprint.toErrorResult()

        MusicSettingsFingerprint.result?.mutableMethod?.replaceInstruction(
            0,
            "const-string v0, \"$MusicDownloaderPackageName\""
        )?: return MusicSettingsFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference("navigation", "revanced_hook_share_button", "false")

        return PatchResultSuccess()
    }
}
