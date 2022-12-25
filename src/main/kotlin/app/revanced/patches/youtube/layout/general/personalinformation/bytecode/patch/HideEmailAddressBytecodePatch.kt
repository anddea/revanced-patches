package app.revanced.patches.youtube.layout.general.personalinformation.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.layout.general.personalinformation.bytecode.fingerprints.AccountSwitcherAccessibilityLabelFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("hide-email-address-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HideEmailAddressBytecodePatch : BytecodePatch(
    listOf(
        AccountSwitcherAccessibilityLabelFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val accountSwitcherAccessibilityLabelResult = AccountSwitcherAccessibilityLabelFingerprint.result!!
        val accountSwitcherAccessibilityLabelMethod = accountSwitcherAccessibilityLabelResult.mutableMethod

        val setVisibilityConstIndex =
            accountSwitcherAccessibilityLabelResult.scanResult.patternScanResult!!.endIndex

        val setVisibilityConstRegister = (
                accountSwitcherAccessibilityLabelMethod.instruction
                (setVisibilityConstIndex - 2) as OneRegisterInstruction
            ).registerA

        accountSwitcherAccessibilityLabelMethod.addInstructions(
            setVisibilityConstIndex, """
            invoke-static {v$setVisibilityConstRegister}, $GENERAL_LAYOUT->hideEmailAddress(I)I
            move-result v$setVisibilityConstRegister
        """
        )

        return PatchResultSuccess()
    }
}
