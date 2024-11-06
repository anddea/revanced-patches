package app.revanced.patches.youtube.utils.fix.bottomui

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fix.bottomui.fingerprints.ExploderControlsFingerprint
import app.revanced.patches.youtube.utils.fix.bottomui.fingerprints.FullscreenButtonPositionFingerprint
import app.revanced.patches.youtube.utils.fix.bottomui.fingerprints.FullscreenButtonViewStubFingerprint
import app.revanced.util.injectLiteralInstructionBooleanCall

@Patch(
    description = "Fixes an issue where overlay button patches were broken by the new layout."
)
object CfBottomUIPatch : BytecodePatch(
    setOf(
        ExploderControlsFingerprint,
        FullscreenButtonPositionFingerprint,
        FullscreenButtonViewStubFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * This issue only affects some versions of YouTube.
         * Therefore, this patch only applies to versions that can resolve this fingerprint.
         */
        mapOf(
            ExploderControlsFingerprint to 45643739,
            FullscreenButtonViewStubFingerprint to 45617294,
            FullscreenButtonPositionFingerprint to 45627640
        ).forEach { (fingerprint, literalValue) ->
            fingerprint.result?.let {
                fingerprint.injectLiteralInstructionBooleanCall(
                    literalValue,
                    "0x0"
                )
            }
        }

    }
}
