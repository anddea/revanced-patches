package app.revanced.patches.youtube.layout.general.widesearchbar.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.widesearchbar.fingerprints.*
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL_LAYOUT

@Patch
@Name("enable-wide-searchbar")
@Description("Replaces the search icon with a wide search bar. This will hide the YouTube logo when active.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class WideSearchbarPatch : BytecodePatch(
    listOf(
        WideSearchbarOneParentFingerprint,
        WideSearchbarTwoParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        arrayOf(
            WideSearchbarOneParentFingerprint to WideSearchbarOneFingerprint,
            WideSearchbarTwoParentFingerprint to WideSearchbarTwoFingerprint
        ).map { (parentFingerprint, fingerprint) ->
            parentFingerprint.result?.classDef?.let { classDef ->
                fingerprint.also { it.resolve(context, classDef) }.result?.let {
                    val index = if (fingerprint == WideSearchbarOneFingerprint) it.scanResult.patternScanResult!!.endIndex
                    else it.scanResult.patternScanResult!!.startIndex

                    val targetMethod =
                        context
                        .toMethodWalker(it.method)
                        .nextMethod(index, true)
                        .getMethod() as MutableMethod

                    injectSearchBarHook(targetMethod)
                } ?: return fingerprint.toErrorResult()
            } ?: return parentFingerprint.toErrorResult()
        }

        /*
        add settings
        */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: LAYOUT_SETTINGS",
                "PREFERENCE_HEADER: GENERAL",
                "SETTINGS: ENABLE_WIDE_SEARCHBAR"
            )
        )

        SettingsPatch.updatePatchStatus("enable-wide-searchbar")

        return PatchResultSuccess()
    }

    private fun injectSearchBarHook(method: MutableMethod) {
        val index = method.implementation!!.instructions.size - 1
        method.addInstructions(
            index, """
            invoke-static {}, $GENERAL_LAYOUT->enableWideSearchbar()Z
            move-result p0
        """
        )
    }
}
