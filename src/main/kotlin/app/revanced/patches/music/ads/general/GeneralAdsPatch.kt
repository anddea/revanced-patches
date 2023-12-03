package app.revanced.patches.music.ads.general

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.ads.general.fingerprints.FloatingLayoutFingerprint
import app.revanced.patches.music.ads.general.fingerprints.NotifierShelfFingerprint
import app.revanced.patches.music.ads.music.MusicAdsPatch
import app.revanced.patches.music.utils.litho.LithoFilterPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.FloatingLayout
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_ADS_PATH
import app.revanced.util.integrations.Constants.MUSIC_COMPONENTS_PATH
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Hide general ads",
    description = "Hides general ads.",
    dependencies = [
        LithoFilterPatch::class,
        MusicAdsPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")]
)
@Suppress("unused")
object GeneralAdsPatch : BytecodePatch(
    setOf(
        FloatingLayoutFingerprint,
        NotifierShelfFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        /**
         * Hides premium promotion popup
         */
        FloatingLayoutFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralIndex(FloatingLayout) + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $MUSIC_ADS_PATH/PremiumPromotionPatch;->hidePremiumPromotion(Landroid/view/View;)V"
                )
            }
        } ?: throw FloatingLayoutFingerprint.exception

        /**
         * Hides premium renewal banner
         */
        NotifierShelfFingerprint.result?.let {
            it.mutableMethod.apply {
                val linearLayoutIndex = it.scanResult.patternScanResult!!.startIndex
                val linearLayoutRegister = getInstruction<FiveRegisterInstruction>(linearLayoutIndex).registerC

                val textViewIndex = linearLayoutIndex + 2
                val textViewRegister = getInstruction<OneRegisterInstruction>(textViewIndex).registerA

                addInstruction(
                    textViewIndex,
                    "invoke-static {v$linearLayoutRegister, v$textViewRegister}, $MUSIC_ADS_PATH/PremiumRenewalPatch;->hidePremiumRenewal(Landroid/widget/LinearLayout;Landroid/view/View;)V"
                )
            }
        } ?: throw NotifierShelfFingerprint.exception

        SettingsPatch.addMusicPreference(CategoryType.ADS, "revanced_close_interstitial_ads", "true")
        SettingsPatch.addMusicPreference(CategoryType.ADS, "revanced_hide_general_ads", "true")
        SettingsPatch.addMusicPreference(CategoryType.ADS, "revanced_hide_music_ads", "true")
        SettingsPatch.addMusicPreference(CategoryType.ADS, "revanced_hide_premium_promotion", "true")
        SettingsPatch.addMusicPreference(CategoryType.ADS, "revanced_hide_premium_renewal", "true")
    }

    private const val FILTER_CLASS_DESCRIPTOR =
        "$MUSIC_COMPONENTS_PATH/AdsFilter;"
}
