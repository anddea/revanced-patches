package app.morphe.patches.reddit.ad

import app.morphe.patches.reddit.utils.extension.Constants
import app.morphe.patches.reddit.utils.patch.PatchList
import app.morphe.patches.reddit.utils.settings.is_2025_06_or_greater
import app.morphe.patches.reddit.utils.settings.settingsPatch
import app.morphe.patches.reddit.utils.settings.updatePatchStatus
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.util.findMutableMethodOf
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstStringInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "${Constants.PATCHES_PATH}/GeneralAdsPatch;"

@Suppress("unused")
val adsPatch = bytecodePatch(
    PatchList.HIDE_ADS.title,
    PatchList.HIDE_ADS.summary,
) {
    compatibleWith(app.morphe.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {
        // region Filter promoted ads (does not work in popular or latest feed)
        adPostFingerprint.methodOrThrow().apply {
            val targetIndex = indexOfFirstInstructionOrThrow {
                getReference<FieldReference>()?.name == "children"
            }
            val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

            addInstructions(
                targetIndex, """
                    invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideOldPostAds(Ljava/util/List;)Ljava/util/List;
                    move-result-object v$targetRegister
                    """
            )
        }

        // The new feeds work by inserting posts into lists.
        // AdElementConverter is conveniently responsible for inserting all feed ads.
        // By removing the appending instruction no ad posts gets appended to the feed.
        val newAdPostMethod = newAdPostFingerprint.second.methodOrNull
            ?: newAdPostLegacyFingerprint.methodOrThrow()

        newAdPostMethod.apply {
            val startIndex =
                0.coerceAtLeast(indexOfFirstStringInstruction("android_feed_freeform_render_variant"))
            val targetIndex = indexOfAddArrayListInstruction(this, startIndex)
            val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

            replaceInstruction(
                targetIndex,
                "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->hideNewPostAds(Ljava/util/ArrayList;Ljava/lang/Object;)V"
            )
        }

        // region Filter comment ads
        fun MutableMethod.hook() =
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hideCommentAds()Z
                    move-result v0
                    if-eqz v0, :show
                    return-void
                    :show
                    nop
                    """
            )
        if (is_2025_06_or_greater) {
            listOf(
                commentAdCommentScreenAdViewFingerprint,
                commentAdDetailListHeaderViewFingerprint,
                commentsViewModelFingerprint
            ).forEach { fingerprint ->
                fingerprint.methodOrThrow().hook()
            }
        } else {
            val isCommentAdsMethod: Method.() -> Boolean = {
                parameterTypes.size == 1 &&
                        parameterTypes.first().startsWith("Lcom/reddit/ads/conversation/") &&
                        accessFlags == AccessFlags.PUBLIC or AccessFlags.FINAL &&
                        returnType == "V" &&
                        indexOfFirstStringInstruction("ad") >= 0
            }

            classDefForEach { classDef ->
                classDef.methods.forEach { method ->
                    if (method.isCommentAdsMethod()) {
                        mutableClassDefBy(classDef)
                            .findMutableMethodOf(method)
                            .hook()
                    }
                }
            }
        }

        updatePatchStatus(
            "enableGeneralAds",
            PatchList.HIDE_ADS
        )
    }
}
