package app.revanced.patches.youtube.layout.general.pivotbar.createbutton.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.MethodFingerprintExtensions.name
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.layout.general.pivotbar.createbutton.bytecode.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.fingerprints.PivotBarFingerprint
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT
import app.revanced.shared.util.pivotbar.InjectionUtils.injectHook
import app.revanced.shared.util.pivotbar.InjectionUtils.REGISTER_TEMPLATE_REPLACEMENT

@Name("hide-create-button-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class CreateButtonRemoverBytecodePatch : BytecodePatch(
    listOf(PivotBarFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        /*
         * Resolve fingerprints
         */

        val pivotBarResult = PivotBarFingerprint.result ?: return PatchResultError("PivotBarFingerprint failed")

        if (!PivotBarCreateButtonViewFingerprint.resolve(context, pivotBarResult.mutableMethod, pivotBarResult.mutableClass))
            return PatchResultError("${PivotBarCreateButtonViewFingerprint.name} failed")

        val createButtonResult = PivotBarCreateButtonViewFingerprint.result!!
        val insertIndex = createButtonResult.scanResult.patternScanResult!!.endIndex

        /*
         * Inject hooks
         */

        val hook =
            "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $GENERAL_LAYOUT->hideCreateButton(Landroid/view/View;)V"

        createButtonResult.mutableMethod.injectHook(hook, insertIndex)

        return PatchResultSuccess()
    }
}
