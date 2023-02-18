package app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.fingerprints.QualityMenuViewInflateFingerprint
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.FLYOUTPANEL_LAYOUT
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch
@Name("enable-old-quality-layout")
@Description("Enables the original quality flyout menu.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class OldQualityLayoutPatch : BytecodePatch(
    listOf(QualityMenuViewInflateFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        QualityMenuViewInflateFingerprint.result?.mutableMethod?.let {
            with (it.implementation!!.instructions) {
                val insertIndex = this.size - 1 - 1
                val register = (this[insertIndex] as FiveRegisterInstruction).registerC

                it.addInstructions(
                    insertIndex, // insert the integrations instructions right before the listener
                    "invoke-static { v$register }, $FLYOUTPANEL_LAYOUT->enableOldQualityMenu(Landroid/widget/ListView;)V"
                )
            }
        } ?: return QualityMenuViewInflateFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: LAYOUT_SETTINGS",
                "PREFERENCE_HEADER: FLYOUT_PANEL",
                "SETTINGS: ENABLE_OLD_QUALITY_LAYOUT"
            )
        )

        SettingsPatch.updatePatchStatus("enable-old-quality-layout")

        return PatchResultSuccess()
    }
}
