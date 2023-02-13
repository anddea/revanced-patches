package app.revanced.patches.music.layout.castbutton.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.layout.castbutton.fingerprints.HideCastButtonFingerprint
import app.revanced.patches.music.layout.castbutton.fingerprints.HideCastButtonParentFingerprint
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.extensions.toErrorResult
import app.revanced.shared.util.integrations.Constants.MUSIC_SETTINGS_PATH

@Patch
@Name("hide-music-cast-button")
@Description("Hides the cast button in the video player and header")
@DependsOn([MusicSettingsPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class HideWatermarkPatch : BytecodePatch(
    listOf(
        HideCastButtonParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        HideCastButtonParentFingerprint.result?.let { parentResult ->
            HideCastButtonFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.addInstructions(
                0, """
                    invoke-static {p1}, $MUSIC_SETTINGS_PATH->hideCastButton(I)I
                    move-result p1
                """
            ) ?: return HideCastButtonFingerprint.toErrorResult()
        } ?: return HideCastButtonParentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
