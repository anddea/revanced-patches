package app.revanced.patches.music.general.castbutton.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.general.castbutton.fingerprints.HideCastButtonFingerprint
import app.revanced.patches.music.general.castbutton.fingerprints.HideCastButtonParentFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_GENERAL

@Patch
@Name("Hide cast button")
@Description("Hides the cast button in the video player and header.")
@DependsOn([SettingsPatch::class])
@MusicCompatibility
class HideCastButtonPatch : BytecodePatch(
    listOf(HideCastButtonParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        HideCastButtonParentFingerprint.result?.let { parentResult ->
            HideCastButtonFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.mutableMethod?.addInstructions(
                0, """
                    invoke-static {p1}, $MUSIC_GENERAL->hideCastButton(I)I
                    move-result p1
                """
            ) ?: throw HideCastButtonFingerprint.exception
        } ?: throw HideCastButtonParentFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_hide_cast_button",
            "true"
        )

    }
}
