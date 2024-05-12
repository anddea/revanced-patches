package app.revanced.patches.youtube.player.flyoutmenu.hide

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.player.flyoutmenu.hide.fingerprints.AdvancedQualityBottomSheetFingerprint
import app.revanced.patches.youtube.player.flyoutmenu.hide.fingerprints.CaptionsBottomSheetFingerprint
import app.revanced.patches.youtube.player.flyoutmenu.hide.fingerprints.PiPModeConfigFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.fingerprints.QualityMenuViewInflateFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.BottomSheetFooterText
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.literalInstructionViewHook
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object PlayerFlyoutMenuPatch : BaseBytecodePatch(
    name = "Hide player flyout menu",
    description = "Adds options to hide player flyout menu components.",
    dependencies = setOf(
        LithoFilterPatch::class,
        PlayerTypeHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AdvancedQualityBottomSheetFingerprint,
        CaptionsBottomSheetFingerprint,
        PiPModeConfigFingerprint,
        QualityMenuViewInflateFingerprint
    )
) {
    private const val PANELS_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/PlayerFlyoutMenuFilter;"

    override fun execute(context: BytecodeContext) {

        arrayOf(
            AdvancedQualityBottomSheetFingerprint to "hideFooterQuality",
            CaptionsBottomSheetFingerprint to "hideFooterCaptions",
            QualityMenuViewInflateFingerprint to "hideFooterQuality"
        ).map { (fingerprint, name) ->
            val smaliInstruction = """
                    invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $PLAYER_CLASS_DESCRIPTOR->$name(Landroid/view/View;)V
                    """
            fingerprint.literalInstructionViewHook(BottomSheetFooterText, smaliInstruction)
        }

        LithoFilterPatch.addFilter(PANELS_FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "PREFERENCE_SCREENS: FLYOUT_MENU",
                "SETTINGS: HIDE_PLAYER_FLYOUT_MENU"
            )
        )

        SettingsPatch.updatePatchStatus(this)

        if (SettingsPatch.upward1839) {
            PiPModeConfigFingerprint.literalInstructionBooleanHook(
                45427407,
                "$PLAYER_CLASS_DESCRIPTOR->hidePiPModeMenu(Z)Z"
            )
            SettingsPatch.addPreference(
                arrayOf(
                    "SETTINGS: HIDE_PIP_MODE_MENU"
                )
            )
        }
    }
}
