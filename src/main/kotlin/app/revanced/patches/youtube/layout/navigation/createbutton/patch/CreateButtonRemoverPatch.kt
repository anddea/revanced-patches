package app.revanced.patches.youtube.layout.navigation.createbutton.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.NAVIGATION
import app.revanced.util.pivotbar.InjectionUtils.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.pivotbar.InjectionUtils.injectHook
import org.jf.dexlib2.Opcode

@Patch
@Name("hide-create-button")
@Description("Hides the create button in the navigation bar.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class CreateButtonRemoverPatch : BytecodePatch(
    listOf(
        PivotBarCreateButtonViewFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        PivotBarCreateButtonViewFingerprint.result?.let { result ->
            with (result.mutableMethod) {
                val insertIndex = implementation!!.instructions.let {
                    val scanStart = result.scanResult.patternScanResult!!.endIndex

                    scanStart + it.subList(scanStart, it.size - 1).indexOfFirst { instruction ->
                        instruction.opcode == Opcode.INVOKE_STATIC
                    }
                }
                injectHook(hook, insertIndex)
            }
        } ?: return PivotBarCreateButtonViewFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: HIDE_CREATE_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("hide-create-button")

        return PatchResultSuccess()
    }

    private companion object {
        const val hook =
            "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $NAVIGATION" +
            "->" +
            "hideCreateButton(Landroid/view/View;)V"
    }
}
