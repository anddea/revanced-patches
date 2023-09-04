package app.revanced.patches.reddit.ad.general.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.annotations.RequiresIntegrations
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.ad.banner.patch.HideBannerPatch
import app.revanced.patches.reddit.ad.comments.patch.HideCommentAdsPatch
import app.revanced.patches.reddit.ad.general.fingerprints.AdPostFingerprint
import app.revanced.patches.reddit.ad.general.fingerprints.NewAdPostFingerprint
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.reddit.utils.settings.bytecode.patch.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.resource.patch.SettingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Patch
@Name("Hide ads")
@Description("Hides ads from the Reddit.")
@DependsOn(
    [
        HideBannerPatch::class,
        HideCommentAdsPatch::class,
        SettingsPatch::class
    ]
)
@RedditCompatibility
@RequiresIntegrations
class HideAdsPatch : BytecodePatch(
    listOf(
        AdPostFingerprint,
        NewAdPostFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        // region Filter promoted ads (does not work in popular or latest feed)

        AdPostFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetReference = getInstruction<ReferenceInstruction>(targetIndex).reference
                val targetReferenceName = (targetReference as FieldReference).name

                if (targetReferenceName != "children")
                    throw PatchException("Method signature reference name did not match: $targetReferenceName")

                val targetRegister = getInstruction<Instruction22c>(targetIndex).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {v$targetRegister}, $INTEGRATIONS_OLD_METHOD_DESCRIPTOR
                        move-result-object v$targetRegister
                        """
                )
            }
        } ?: throw AdPostFingerprint.exception

        // The new feeds work by inserting posts into lists.
        // AdElementConverter is conveniently responsible for inserting all feed ads.
        // By removing the appending instruction no ad posts gets appended to the feed.
        NewAdPostFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetParameter =
                    getInstruction<ReferenceInstruction>(targetIndex).reference.toString()

                if (!targetParameter.endsWith("Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z"))
                    throw PatchException("Method signature parameter did not match: $targetParameter")

                val targetRegister =
                    getInstruction<FiveRegisterInstruction>(targetIndex).registerD + 1

                addInstructionsWithLabels(
                    targetIndex, """
                        invoke-static {}, $INTEGRATIONS_NEW_METHOD_DESCRIPTOR
                        move-result v$targetRegister
                        if-nez v$targetRegister, :show
                        """, ExternalLabel("show", getInstruction(targetIndex + 1))
                )
            }
        } ?: throw NewAdPostFingerprint.exception

        updateSettingsStatus("GeneralAds")

    }

    private companion object {
        private const val INTEGRATIONS_OLD_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/patches/GeneralAdsPatch;" +
                    "->hideOldPostAds(Ljava/util/List;)Ljava/util/List;"

        private const val INTEGRATIONS_NEW_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/patches/GeneralAdsPatch;" +
                    "->hideNewPostAds()Z"
    }
}
