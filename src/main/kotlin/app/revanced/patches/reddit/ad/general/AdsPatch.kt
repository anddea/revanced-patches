package app.revanced.patches.reddit.ad.general

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patches.reddit.ad.banner.BannerAdsPatch
import app.revanced.patches.reddit.ad.comments.CommentAdsPatch
import app.revanced.patches.reddit.ad.general.fingerprints.AdPostFingerprint
import app.revanced.patches.reddit.ad.general.fingerprints.NewAdPostFingerprint
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndexWithFieldReferenceName
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
object AdsPatch : BaseBytecodePatch(
    name = "Hide ads",
    description = "Adds options to hide ads.",
    dependencies = setOf(
        BannerAdsPatch::class,
        CommentAdsPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AdPostFingerprint,
        NewAdPostFingerprint
    ),
    requiresIntegrations = true
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$PATCHES_PATH/GeneralAdsPatch;"

    override fun execute(context: BytecodeContext) {
        // region Filter promoted ads (does not work in popular or latest feed)
        AdPostFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getTargetIndexWithFieldReferenceName("children")
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->hideOldPostAds(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$targetRegister
                        """
                )
            }
        }

        // The new feeds work by inserting posts into lists.
        // AdElementConverter is conveniently responsible for inserting all feed ads.
        // By removing the appending instruction no ad posts gets appended to the feed.
        NewAdPostFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getTargetIndexWithMethodReferenceName("add")
                val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

                replaceInstruction(
                    targetIndex,
                    "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                            "$INTEGRATIONS_CLASS_DESCRIPTOR->hideNewPostAds(Ljava/util/ArrayList;Ljava/lang/Object;)V"
                )
            }
        }

        updateSettingsStatus("enableGeneralAds")

    }
}
