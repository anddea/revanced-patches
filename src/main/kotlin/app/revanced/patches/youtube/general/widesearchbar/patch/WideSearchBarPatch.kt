package app.revanced.patches.youtube.general.widesearchbar.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.general.widesearchbar.fingerprints.SetActionBarRingoFingerprint
import app.revanced.patches.youtube.utils.fingerprints.SetToolBarPaddingFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch.Companion.contexts
import app.revanced.util.integrations.Constants.GENERAL

@Patch
@Name("enable-wide-search-bar")
@Description("Replaces the search icon with a wide search bar. This will hide the YouTube logo when active.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class WideSearchBarPatch : BytecodePatch(
    listOf(
        SetActionBarRingoFingerprint,
        SetToolBarPaddingFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        arrayOf(
            SetActionBarRingoFingerprint,
            SetToolBarPaddingFingerprint
        ).forEach {
            it.injectHook(context)
        }

        /**
         * Set Wide SearchBar Start Margin
         */
        contexts.xmlEditor[TARGET_RESOURCE_PATH].use { editor ->
            val document = editor.file

            with(document.getElementsByTagName("RelativeLayout").item(0)) {
                if (attributes.getNamedItem(FLAG) != null) return@with

                document.createAttribute(FLAG)
                    .apply { value = "8.0dip" }
                    .let(attributes::setNamedItem)
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: ENABLE_WIDE_SEARCH_BAR"
            )
        )

        SettingsPatch.updatePatchStatus("enable-wide-search-bar")

        return PatchResultSuccess()
    }

    private companion object {
        const val FLAG = "android:paddingStart"
        const val TARGET_RESOURCE_PATH = "res/layout/action_bar_ringo_background.xml"

        fun MethodFingerprint.injectHook(context: BytecodeContext) {
            result?.let {
                (context
                    .toMethodWalker(it.method)
                    .nextMethod(it.scanResult.patternScanResult!!.endIndex, true)
                    .getMethod() as MutableMethod).apply {
                    addInstructions(
                        implementation!!.instructions.size - 1, """
                            invoke-static {}, $GENERAL->enableWideSearchBar()Z
                            move-result p0
                            """
                    )
                }
            } ?: throw toErrorResult()
        }
    }
}
