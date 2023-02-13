package app.revanced.patches.youtube.extended.layoutswitch.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.LayoutSwitchFingerprint
import app.revanced.util.integrations.Constants.EXTENDED_PATH

@Name("layout-switch-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class LayoutSwitchBytecodePatch : BytecodePatch(
    listOf(
        LayoutSwitchFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        LayoutSwitchFingerprint.result?.mutableMethod?.addInstructions(
            4, """
                invoke-static {p0}, $EXTENDED_PATH/LayoutOverridePatch;->getLayoutOverride(I)I
                move-result p0
                """
        ) ?: return LayoutSwitchFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
