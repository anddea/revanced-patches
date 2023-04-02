package app.revanced.patches.youtube.layout.player.infocards.patch

import app.revanced.extensions.toErrorResult
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
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.player.infocards.fingerprints.InfocardsIncognitoFingerprint
import app.revanced.patches.youtube.layout.player.infocards.fingerprints.InfocardsIncognitoParentFingerprint
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER

@Patch
@Name("hide-info-cards")
@Description("Hides info-cards in videos.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class HideInfocardsPatch : BytecodePatch(
    listOf(InfocardsIncognitoParentFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        InfocardsIncognitoParentFingerprint.result?.classDef?.let { classDef ->
            InfocardsIncognitoFingerprint.also {
                it.resolve(context, classDef)
            }.result?.mutableMethod?.
            addInstructions(
                1, """
                    invoke-static {v0}, $PLAYER->hideInfoCard(Z)Z
                    move-result v0
                    """
            ) ?: return InfocardsIncognitoFingerprint.toErrorResult()
        } ?: return InfocardsIncognitoParentFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_INFO_CARDS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-info-cards")

        return PatchResultSuccess()
    }
}