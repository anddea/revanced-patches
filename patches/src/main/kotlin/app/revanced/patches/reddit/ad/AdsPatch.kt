package app.revanced.patches.reddit.ad

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.patch.PatchList.HIDE_ADS
import app.revanced.patches.reddit.utils.settings.settingsPatch
import app.revanced.patches.reddit.utils.settings.updatePatchStatus
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val RESOURCE_FILE_PATH = "res/layout/merge_listheader_link_detail.xml"

private val bannerAdsPatch = resourcePatch(
    description = "bannerAdsPatch",
) {
    execute {
        document(RESOURCE_FILE_PATH).use { document ->
            document.getElementsByTagName("merge").item(0).childNodes.apply {
                val attributes = arrayOf("height", "width")

                for (i in 1 until length) {
                    val view = item(i)
                    if (
                        view.hasAttributes() &&
                        view.attributes.getNamedItem("android:id").nodeValue.endsWith("ad_view_stub")
                    ) {
                        attributes.forEach { attribute ->
                            view.attributes.getNamedItem("android:layout_$attribute").nodeValue =
                                "0.0dip"
                        }

                        break
                    }
                }
            }
        }
    }
}

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$PATCHES_PATH/GeneralAdsPatch;->hideCommentAds()Z"

private val commentAdsPatch = bytecodePatch(
    description = "commentAdsPatch",
) {
    execute {
        commentAdsFingerprint.matchOrThrow().let {
            val walkerMethod = it.getWalkerMethod(it.patternMatch!!.startIndex)
            walkerMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $EXTENSION_METHOD_DESCRIPTOR
                        move-result v0
                        if-eqz v0, :show
                        new-instance v0, Ljava/lang/Object;
                        invoke-direct {v0}, Ljava/lang/Object;-><init>()V
                        return-object v0
                        """, ExternalLabel("show", getInstruction(0))
                )
            }
        }
    }
}

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/GeneralAdsPatch;"

@Suppress("unused")
val adsPatch = bytecodePatch(
    HIDE_ADS.title,
    HIDE_ADS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        bannerAdsPatch,
        commentAdsPatch,
        settingsPatch
    )

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
        newAdPostFingerprint.methodOrThrow().apply {
            val stringIndex = indexOfFirstStringInstructionOrThrow("android_feed_freeform_render_variant")
            val targetIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                opcode == Opcode.INVOKE_VIRTUAL
                        && getReference<MethodReference>()?.toString() == "Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z"
            }
            val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

            replaceInstruction(
                targetIndex,
                "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->hideNewPostAds(Ljava/util/ArrayList;Ljava/lang/Object;)V"
            )
        }

        updatePatchStatus(
            "enableGeneralAds",
            HIDE_ADS
        )
    }
}
