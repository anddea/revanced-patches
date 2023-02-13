package app.revanced.patches.youtube.layout.player.infocards.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.player.infocards.bytecode.fingerprints.InfocardsIncognitoFingerprint
import app.revanced.patches.youtube.layout.player.infocards.bytecode.fingerprints.InfocardsIncognitoParentFingerprint
import app.revanced.util.integrations.Constants.PLAYER_LAYOUT

@Name("hide-info-cards-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HideInfoCardsBytecodePatch : BytecodePatch(
    listOf(InfocardsIncognitoParentFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        InfocardsIncognitoParentFingerprint.result?.classDef?.let { classDef ->
            InfocardsIncognitoFingerprint.also {
                it.resolve(context, classDef)
            }.result?.mutableMethod?.
            addInstructions(
                1, """
                    invoke-static {v0}, $PLAYER_LAYOUT->hideInfoCard(Z)Z
                    move-result v0
                    """
            ) ?: return InfocardsIncognitoFingerprint.toErrorResult()
        } ?: return InfocardsIncognitoParentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}