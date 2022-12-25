package app.revanced.patches.youtube.layout.general.startupshortsreset.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.layout.general.startupshortsreset.bytecode.fingerprints.UserWasInShortsFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT

@Name("hide-startup-shorts-player-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HideShortsOnStartupBytecodePatch : BytecodePatch(
    listOf(
        UserWasInShortsFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val userWasInShortsResult = UserWasInShortsFingerprint.result!!
        val userWasInShortsMethod = userWasInShortsResult.mutableMethod
        val moveResultIndex = userWasInShortsResult.scanResult.patternScanResult!!.endIndex

        userWasInShortsMethod.addInstructions(
            moveResultIndex + 1, """
            invoke-static { }, $GENERAL_LAYOUT->hideStartupShortsPlayer()Z
            move-result v5
            if-eqz v5, :show_startup_shorts_player
            return-void
            """, listOf(ExternalLabel("show_startup_shorts_player", userWasInShortsMethod.instruction(moveResultIndex + 1)))
        )

        return PatchResultSuccess()
    }
}
