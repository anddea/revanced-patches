package app.revanced.patches.youtube.misc.layoutswitch.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.LayoutSwitchFingerprint
import app.revanced.patches.youtube.misc.layoutswitch.fingerprints.*
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH

@Patch
@Name("layout-switch")
@Description("Tricks the dpi to use some tablet/phone layouts.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class LayoutSwitchPatch : BytecodePatch(
    listOf(
        ClientFormFactorParentFingerprint,
        LayoutSwitchFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        ClientFormFactorParentFingerprint.result?.let { parentResult ->
            ClientFormFactorFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                with (it.mutableMethod) {
                    val jumpIndex = it.scanResult.patternScanResult!!.startIndex + 1
                    addInstructions(
                        1, """
                            invoke-static {}, $MISC_PATH/LayoutOverridePatch;->enableTabletLayout()Z
                            move-result v2
                            if-nez v2, :tablet_layout
                            """, listOf(ExternalLabel("tablet_layout", instruction(jumpIndex)))
                    )
                }
            } ?: return ClientFormFactorFingerprint.toErrorResult()
        } ?: return ClientFormFactorParentFingerprint.toErrorResult()

        LayoutSwitchFingerprint.result?.mutableMethod?.addInstructions(
            4, """
                invoke-static {p0}, $MISC_PATH/LayoutOverridePatch;->getLayoutOverride(I)I
                move-result p0
                """
        ) ?: return LayoutSwitchFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: LAYOUT_SWITCH"
            )
        )

        SettingsPatch.updatePatchStatus("layout-switch")

        return PatchResultSuccess()
    }
}
