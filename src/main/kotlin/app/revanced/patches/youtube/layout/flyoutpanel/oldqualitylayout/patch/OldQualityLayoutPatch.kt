package app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.fingerprints.QualityMenuViewInflateFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.misc.videoid.legacy.patch.LegacyVideoIdPatch
import app.revanced.util.integrations.Constants.FLYOUT_PANEL
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("enable-old-quality-layout")
@Description("Enables the original quality flyout menu.")
@DependsOn(
    [
        LegacyVideoIdPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class OldQualityLayoutPatch : BytecodePatch(
    listOf(
        QualityMenuViewInflateFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        QualityMenuViewInflateFingerprint.result?.let {
            with (it.mutableMethod) {
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val register = (instruction(endIndex) as OneRegisterInstruction).registerA
                addInstruction(
                    endIndex + 1,
                    "invoke-static { v$register }, $FLYOUT_PANEL->enableOldQualityMenu(Landroid/widget/ListView;)V"
                )
            }
        } ?: return QualityMenuViewInflateFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FLYOUT_PANEL_SETTINGS",
                "SETTINGS: ENABLE_OLD_QUALITY_LAYOUT"
            )
        )

        SettingsPatch.updatePatchStatus("enable-old-quality-layout")

        return PatchResultSuccess()
    }
}
