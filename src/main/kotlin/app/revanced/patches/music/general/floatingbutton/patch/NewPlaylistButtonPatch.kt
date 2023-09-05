package app.revanced.patches.music.general.floatingbutton.patch

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
import app.revanced.patches.music.general.floatingbutton.fingerprints.FloatingButtonFingerprint
import app.revanced.patches.music.general.floatingbutton.fingerprints.FloatingButtonParentFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_GENERAL

@Patch
@Name("Hide new playlist button")
@Description("Hide the \"New playlist\" button in the library.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@MusicCompatibility
class NewPlaylistButtonPatch : BytecodePatch(
    listOf(FloatingButtonParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        FloatingButtonParentFingerprint.result?.let { parentResult ->
            FloatingButtonFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    addInstructionsWithLabels(
                        1, """
                            invoke-static {}, $MUSIC_GENERAL->hideNewPlaylistButton()Z
                            move-result v0
                            if-eqz v0, :show
                            return-void
                            """, ExternalLabel("show", getInstruction(1))
                    )
                }
            } ?: throw FloatingButtonFingerprint.exception
        } ?: throw FloatingButtonParentFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_hide_new_playlist_button",
            "false"
        )

    }
}
