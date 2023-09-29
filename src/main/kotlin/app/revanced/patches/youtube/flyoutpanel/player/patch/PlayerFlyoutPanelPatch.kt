package app.revanced.patches.youtube.flyoutpanel.player.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.flyoutpanel.player.fingerprints.AdvancedQualityBottomSheetFingerprint
import app.revanced.patches.youtube.flyoutpanel.player.fingerprints.CaptionsBottomSheetFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.QualityMenuViewInflateFingerprint
import app.revanced.patches.youtube.utils.litho.patch.LithoFilterPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.BottomSheetFooterText
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.FLYOUT_PANEL
import app.revanced.util.integrations.Constants.PATCHES_PATH
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Hide player flyout panel")
@Description("Hides player flyout panel components.")
@DependsOn(
    [
        LithoFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class PlayerFlyoutPanelPatch : BytecodePatch(
    listOf(
        AdvancedQualityBottomSheetFingerprint,
        CaptionsBottomSheetFingerprint,
        QualityMenuViewInflateFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        arrayOf(
            AdvancedQualityBottomSheetFingerprint to "hideFooterQuality",
            CaptionsBottomSheetFingerprint to "hideFooterCaptions",
            QualityMenuViewInflateFingerprint to "hideFooterQuality"
        ).map { (fingerprint, name) ->
            fingerprint.injectCall(name)
        }

        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/PlayerFlyoutPanelsFilter;")
        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/PlayerFlyoutPanelsFooterFilter;")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FLYOUT_PANEL_SETTINGS",
                "SETTINGS: PLAYER_FLYOUT_PANEL_HEADER",
                "SETTINGS: HIDE_PLAYER_FLYOUT_PANEL"
            )
        )

        SettingsPatch.updatePatchStatus("hide-player-flyout-panel")

    }

    private companion object {
        fun MethodFingerprint.injectCall(descriptor: String) {
            result?.let {
                it.mutableMethod.apply {
                    val insertIndex = getWideLiteralIndex(BottomSheetFooterText) + 3
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstruction(
                        insertIndex,
                        "invoke-static {v$insertRegister}, $FLYOUT_PANEL->$descriptor(Landroid/view/View;)V"
                    )
                }
            } ?: throw exception
        }
    }
}
