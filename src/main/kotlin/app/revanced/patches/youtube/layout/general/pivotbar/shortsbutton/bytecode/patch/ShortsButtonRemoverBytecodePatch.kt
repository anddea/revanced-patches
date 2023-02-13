package app.revanced.patches.youtube.layout.general.pivotbar.shortsbutton.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.patches.youtube.layout.general.pivotbar.shortsbutton.bytecode.fingerprints.PivotBarEnumFingerprint
import app.revanced.patches.youtube.layout.general.pivotbar.shortsbutton.bytecode.fingerprints.PivotBarShortsButtonViewFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.util.integrations.Constants.GENERAL_LAYOUT
import app.revanced.util.pivotbar.InjectionUtils.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.pivotbar.InjectionUtils.injectHook

@Name("hide-shorts-button")
@DependsOn([SharedResourcdIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class ShortsButtonRemoverBytecodePatch : BytecodePatch(
    listOf(PivotBarCreateButtonViewFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        /*
         * Resolve fingerprints
         */

        PivotBarCreateButtonViewFingerprint.result?.let { parentResult ->
            with (
                arrayOf(
                    PivotBarEnumFingerprint,
                    PivotBarShortsButtonViewFingerprint
                ).onEach {
                    it.resolve(
                        context,
                        parentResult.mutableMethod,
                        parentResult.mutableClass
                    )
                }.map {
                    it.result?.scanResult?.patternScanResult ?: return it.toErrorResult()
                }
            ) {
                val enumScanResult = this[0]
                val buttonViewResult = this[1]

                val enumHookInsertIndex = enumScanResult.startIndex + 2
                val buttonHookInsertIndex = buttonViewResult.endIndex

                mapOf(
                    buttonHook to buttonHookInsertIndex,
                    enumHook to enumHookInsertIndex
                ).forEach { (hook, insertIndex) ->
                    parentResult.mutableMethod.injectHook(hook, insertIndex)
                }
            }

        } ?: return PivotBarCreateButtonViewFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
    private companion object {
        const val enumHook =
            "sput-object v$REGISTER_TEMPLATE_REPLACEMENT, $GENERAL_LAYOUT" +
            "->" +
            "lastPivotTab:Ljava/lang/Enum;"

        const val buttonHook =
            "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $GENERAL_LAYOUT" +
            "->" +
            "hideShortsButton(Landroid/view/View;)V"
    }
}