package app.revanced.patches.music.layout.floatingbutton.patch

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
import app.revanced.patches.music.layout.floatingbutton.fingerprints.FloatingButtonFingerprint
import app.revanced.patches.music.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.integrations.Constants.MUSIC_SETTINGS_PATH
import org.jf.dexlib2.iface.instruction.formats.Instruction35c

@Patch
@Name("hide-new-playlist")
@Description("Hide the New Playlist button in the Library tab.")
@DependsOn(
    [
        MusicSettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeMusicCompatibility
@Version("0.0.1")
class NewPlaylistButtonPatch : BytecodePatch(
    listOf(
        FloatingButtonFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        FloatingButtonFingerprint.result?.let {
            with (it.mutableMethod) {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex

                val targetRegister = (instruction(targetIndex) as Instruction35c).registerC
                val dummyRegister = (instruction(targetIndex) as Instruction35c).registerD

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {}, $MUSIC_SETTINGS_PATH->hideNewPlaylistButton()Z
                        move-result v$dummyRegister
                        if-eqz v$dummyRegister, :show
                        const/4 v$targetRegister, 0x0
                        """, listOf(ExternalLabel("show", instruction(targetIndex + 1)))
                )
            }
        } ?: return FloatingButtonFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference("navigation", "revanced_hide_new_playlist_button", "false")

        return PatchResultSuccess()
    }
}
