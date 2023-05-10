package app.revanced.patches.youtube.layout.shorts.startupshortsreset.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.shorts.startupshortsreset.fingerprints.UserWasInShortsFingerprint
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.SHORTS
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("disable-startup-shorts-player")
@Description("Disables playing YouTube Shorts when launching YouTube.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class DisableShortsOnStartupPatch : BytecodePatch(
    listOf(UserWasInShortsFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        UserWasInShortsFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex + 1
                val register = instruction<OneRegisterInstruction>(insertIndex - 1).registerA + 2
                addInstructions(
                    insertIndex, """
                        invoke-static { }, $SHORTS->disableStartupShortsPlayer()Z
                        move-result v$register
                        if-eqz v$register, :show_startup_shorts_player
                        return-void
                    """, listOf(ExternalLabel("show_startup_shorts_player", instruction(insertIndex)))
                )
            }
        } ?: return UserWasInShortsFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SHORTS_SETTINGS",
                "SETTINGS: SHORTS_PLAYER_PARENT",
                "SETTINGS: DISABLE_STARTUP_SHORTS_PLAYER"
            )
        )

        SettingsPatch.updatePatchStatus("disable-startup-shorts-player")

        return PatchResultSuccess()
    }
}
