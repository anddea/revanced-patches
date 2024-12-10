package app.revanced.patches.youtube.utils.fix.bottomui

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.resolvable

val cfBottomUIPatch = bytecodePatch(
    description = "cfBottomUIPatch"
) {
    execute {
        /**
         * This issue only affects some versions of YouTube.
         * Therefore, this patch only applies to versions that can resolve this fingerprint.
         */
        mapOf(
            exploderControlsFingerprint to 45643739L,
            fullscreenButtonViewStubFingerprint to 45617294L,
            fullscreenButtonPositionFingerprint to 45627640L
        ).forEach { (fingerprint, literalValue) ->
            if (fingerprint.resolvable()) {
                fingerprint.injectLiteralInstructionBooleanCall(
                    literalValue,
                    "0x0"
                )
            }
        }
    }
}
