package app.revanced.patches.youtube.layout.general.startupshortsreset.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.startupshortsreset.bytecode.fingerprints.UserWasInShortsFingerprint
import app.revanced.util.integrations.Constants.GENERAL_LAYOUT
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("hide-startup-shorts-player-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HideShortsOnStartupBytecodePatch : BytecodePatch(
    listOf(
        UserWasInShortsFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        UserWasInShortsFingerprint.result?.let {
            val insertIndex = it.scanResult.patternScanResult!!.endIndex + 1

            with (it.mutableMethod) {
                val register = (instruction(insertIndex - 1) as OneRegisterInstruction).registerA + 2
                addInstructions(
                    insertIndex, """
                        invoke-static { }, $GENERAL_LAYOUT->hideStartupShortsPlayer()Z
                        move-result v$register
                        if-eqz v$register, :show_startup_shorts_player
                        return-void
                    """, listOf(ExternalLabel("show_startup_shorts_player", instruction(insertIndex)))
                )
            }
        } ?: return UserWasInShortsFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
