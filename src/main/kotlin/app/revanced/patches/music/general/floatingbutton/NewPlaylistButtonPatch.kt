package app.revanced.patches.music.general.floatingbutton

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.general.floatingbutton.fingerprints.FloatingButtonFingerprint
import app.revanced.patches.music.general.floatingbutton.fingerprints.FloatingButtonParentFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_GENERAL

@Patch(
    name = "Hide new playlist button",
    description = "Hides the \"New playlist\" button in the library.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.27.54",
                "6.28.52"
            ]
        )
    ]
)
@Suppress("unused")
object NewPlaylistButtonPatch : BytecodePatch(
    setOf(FloatingButtonParentFingerprint)
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
