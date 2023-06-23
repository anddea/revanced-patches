package app.revanced.patches.reddit.ad.general.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.ad.banner.patch.HideBannerPatch
import app.revanced.patches.reddit.ad.comments.patch.HideCommentAdsPatch
import app.revanced.patches.reddit.ad.general.fingerprints.AdPostFingerprint
import app.revanced.patches.reddit.ad.general.fingerprints.NewAdPostFingerprint
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.patch.IntegrationsPatch
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction22c
import org.jf.dexlib2.iface.reference.FieldReference

@Patch
@Name("hide-ads")
@Description("Removes ads from the Reddit.")
@DependsOn(
    [
        HideBannerPatch::class,
        HideCommentAdsPatch::class,
        IntegrationsPatch::class
    ]
)
@RedditCompatibility
@Version("0.0.2")
class HideAdsPatch : BytecodePatch(
    listOf(
        AdPostFingerprint,
        NewAdPostFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        // region Filter promoted ads (does not work in popular or latest feed)

        AdPostFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetReference = getInstruction<ReferenceInstruction>(targetIndex).reference
                val targetReferenceName = (targetReference as FieldReference).name

                if (targetReferenceName != "children")
                    throw PatchResultError("Method signature reference name did not match: $targetReferenceName")

                val castedInstruction = getInstruction<Instruction22c>(targetIndex)

                removeInstruction(targetIndex)
                addInstructions(
                    targetIndex, """
                        invoke-static {v${castedInstruction.registerA}}, $FILTER_METHOD_DESCRIPTOR
                        move-result-object v0
                        iput-object v0, v${castedInstruction.registerB}, ${castedInstruction.reference}
                        """
                )
            }
        } ?: return AdPostFingerprint.toErrorResult()

        // The new feeds work by inserting posts into lists.
        // AdElementConverter is conveniently responsible for inserting all feed ads.
        // By removing the appending instruction no ad posts gets appended to the feed.
        NewAdPostFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetParameter =
                    getInstruction<ReferenceInstruction>(targetIndex).reference.toString()

                if (!targetParameter.endsWith("Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z"))
                    throw PatchResultError("Method signature parameter did not match: $targetParameter")

                removeInstruction(targetIndex)
            }
        } ?: return NewAdPostFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    private companion object {
        private const val FILTER_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/patches/FilterPromotedLinksPatch;" +
                    "->filterChildren(Ljava/lang/Iterable;)Ljava/util/List;"
    }
}
