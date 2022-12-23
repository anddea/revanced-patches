package app.revanced.patches.youtube.layout.general.pivotbar.shortsbutton.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.MethodFingerprintExtensions.name
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.layout.general.pivotbar.shortsbutton.bytecode.fingerprints.PivotBarEnumFingerprint
import app.revanced.patches.youtube.layout.general.pivotbar.shortsbutton.bytecode.fingerprints.PivotBarShortsButtonViewFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.fingerprints.PivotBarFingerprint
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT
import app.revanced.shared.util.pivotbar.InjectionUtils.injectHook
import app.revanced.shared.util.pivotbar.InjectionUtils.REGISTER_TEMPLATE_REPLACEMENT

@Name("hide-shorts-button")
@YouTubeCompatibility
@Version("0.0.1")
class ShortsButtonRemoverBytecodePatch : BytecodePatch(
    listOf(PivotBarFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        /*
         * Resolve fingerprints
         */

        val pivotBarResult = PivotBarFingerprint.result ?: return PatchResultError("PivotBarFingerprint failed")
        val fingerprintResults = arrayOf(PivotBarEnumFingerprint, PivotBarShortsButtonViewFingerprint)
            .onEach {
                val resolutionSucceeded = it.resolve(
                    context,
                    pivotBarResult.mutableMethod,
                    pivotBarResult.mutableClass
                )

                if (!resolutionSucceeded) return PatchResultError("${it.name} failed")
            }
            .map { it.result!!.scanResult.patternScanResult!! }

        val enumScanResult = fingerprintResults[0]
        val buttonViewResult = fingerprintResults[1]

        val enumHookInsertIndex = enumScanResult.startIndex + 2
        val buttonHookInsertIndex = buttonViewResult.endIndex

        /*
         * Inject hooks
         */

        val enumHook =
            "sput-object v$REGISTER_TEMPLATE_REPLACEMENT, $GENERAL_LAYOUT->lastPivotTab:Ljava/lang/Enum;"
        val buttonHook =
            "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $GENERAL_LAYOUT->hideShortsButton(Landroid/view/View;)V"

        // Inject bottom to top to not mess up the indices
        mapOf(
            buttonHook to buttonHookInsertIndex,
            enumHook to enumHookInsertIndex
        ).forEach { (hook, insertIndex) ->
            pivotBarResult.mutableMethod.injectHook(hook, insertIndex)
        }

        return PatchResultSuccess()
    }
}