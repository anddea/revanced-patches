package app.revanced.patches.youtube.layout.navigation.shortsbutton.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.patches.youtube.layout.navigation.shortsbutton.fingerprints.PivotBarEnumFingerprint
import app.revanced.patches.youtube.layout.navigation.shortsbutton.fingerprints.PivotBarShortsButtonViewFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.NAVIGATION
import app.revanced.util.pivotbar.InjectionUtils.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.pivotbar.InjectionUtils.injectHook

@Patch
@Name("hide-shorts-button")
@Description("Hides the shorts button in the navigation bar.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ShortsButtonRemoverPatch : BytecodePatch(
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

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: HIDE_SHORTS_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("hide-shorts-button")

        return PatchResultSuccess()
    }
    private companion object {
        const val enumHook =
            "sput-object v$REGISTER_TEMPLATE_REPLACEMENT, $NAVIGATION" +
            "->" +
            "lastPivotTab:Ljava/lang/Enum;"

        const val buttonHook =
            "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $NAVIGATION" +
            "->" +
            "hideShortsButton(Landroid/view/View;)V"
    }
}