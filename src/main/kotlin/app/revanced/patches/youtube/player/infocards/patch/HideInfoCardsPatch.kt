package app.revanced.patches.youtube.player.infocards.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.player.infocards.fingerprints.InfoCardsIncognitoFingerprint
import app.revanced.patches.youtube.player.infocards.fingerprints.InfoCardsIncognitoParentFingerprint
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("hide-info-cards")
@Description("Hides info-cards in videos.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class HideInfoCardsPatch : BytecodePatch(
    listOf(InfoCardsIncognitoParentFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        InfoCardsIncognitoParentFingerprint.result?.classDef?.let { classDef ->
            InfoCardsIncognitoFingerprint.also { it.resolve(context, classDef) }.result?.let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex
                    val targetRegister =
                        getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {v$targetRegister}, $PLAYER->hideInfoCard(Z)Z
                            move-result v$targetRegister
                            """
                    )
                }
            } ?: return InfoCardsIncognitoFingerprint.toErrorResult()
        } ?: return InfoCardsIncognitoParentFingerprint.toErrorResult()

        /**
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