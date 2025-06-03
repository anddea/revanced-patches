package app.revanced.patches.youtube.shorts.components

import app.revanced.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.revanced.patcher.fingerprint
import app.revanced.patches.youtube.utils.resourceid.*
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.literal
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val bottomSheetMenuDismissFingerprint = legacyFingerprint(
    name = "bottomSheetMenuDismissFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    customFingerprint = { method, _ ->
        indexOfDismissInstruction(method) >= 0
    }
)

fun indexOfDismissInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        reference?.name == "dismiss" &&
                reference.returnType == "V" &&
                reference.parameterTypes.isEmpty()
    }

internal val bottomSheetMenuItemClickFingerprint = legacyFingerprint(
    name = "bottomSheetMenuItemClickFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("Landroid/widget/AdapterView;", "Landroid/view/View;", "I", "J"),
    customFingerprint = { method, _ ->
        method.name == "onItemClick"
    }
)

internal val bottomSheetMenuListBuilderFingerprint = legacyFingerprint(
    name = "bottomSheetMenuListBuilderFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
    ),
    strings = listOf("Bottom Sheet Menu is empty. No menu items were supported."),
)

internal val liveHeaderElementsContainerFingerprint = legacyFingerprint(
    name = "liveHeaderElementsContainerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/ViewGroup;", "L"),
    strings = listOf("Header container is null, header cannot be presented."),
    customFingerprint = { method, _ ->
        indexOfAddLiveHeaderElementsContainerInstruction(method) >= 0
    },
)

fun indexOfAddLiveHeaderElementsContainerInstruction(method: Method) =
    method.indexOfFirstInstruction {
        getReference<MethodReference>()?.name == "addView"
    }

internal val reelEnumConstructorFingerprint = legacyFingerprint(
    name = "reelEnumConstructorFingerprint",
    returnType = "V",
    strings = listOf(
        "REEL_LOOP_BEHAVIOR_UNKNOWN",
        "REEL_LOOP_BEHAVIOR_SINGLE_PLAY",
        "REEL_LOOP_BEHAVIOR_REPEAT",
        "REEL_LOOP_BEHAVIOR_END_SCREEN"
    )
)

internal val reelEnumStaticFingerprint = legacyFingerprint(
    name = "reelEnumStaticFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("I"),
    returnType = "L"
)

/**
 * YouTube 18.49.36 ~
 */
internal val reelPlaybackRepeatFingerprint = legacyFingerprint(
    name = "reelPlaybackRepeatFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    strings = listOf("YoutubePlayerState is in throwing an Error.")
)

/**
 * YouTube 20.16+
 */
// internal val reelPlaybackRepeatFingerprint2016 = fingerprint {
//     returns("V")
//     parameters("L")
//     opcodes(
//         Opcode.INVOKE_STATIC,
//         Opcode.MOVE_RESULT_OBJECT,
//         Opcode.IPUT_OBJECT
//     )
//     custom { _, classDef ->
//         classDef.methods.any { classMethod ->
//             classMethod.instructionsOrNull?.any { instruction ->
//                 instruction.opcode == Opcode.CONST_STRING &&
//                         (instruction as? ReferenceInstruction)?.reference?.toString() == "Reels[%s] Playback Time: %d ms"
//             } == true
//         }
//     }
// }

internal val reelPlaybackRepeatFingerprint2016 = legacyFingerprint(
    name = "reelPlaybackRepeatFingerprint2016",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IPUT_OBJECT
    ),
    customFingerprint = { _, classDef ->
        classDef.methods.any { classMethod ->
            classMethod.instructionsOrNull?.any { instruction ->
                instruction.opcode == Opcode.CONST_STRING &&
                        (instruction as? ReferenceInstruction)?.reference?.toString() == "Reels[%s] Playback Time: %d ms"
            } == true
        }
    }
)

internal val reelPlaybackFingerprint = legacyFingerprint(
    name = "reelPlaybackFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("J"),
    returnType = "V",
    customFingerprint = { method, _ ->
        indexOfMilliSecondsInstruction(method) >= 0 &&
                indexOfInitializationInstruction(method) >= 0
    }
)

private fun indexOfMilliSecondsInstruction(method: Method) =
    method.indexOfFirstInstruction {
        getReference<FieldReference>()?.name == "MILLISECONDS"
    }

internal fun indexOfInitializationInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_DIRECT &&
                reference?.name == "<init>" &&
                reference.parameterTypes.size == 3 &&
                reference.parameterTypes.firstOrNull() == "I"
    }

internal const val SHORTS_HUD_FEATURE_FLAG = 45644023L

/**
 * Fix [HUD is hidden](https://github.com/ReVanced/revanced-patches/issues/4426)
 */
internal val shortsHUDFeatureFingerprint = legacyFingerprint(
    name = "shortsHUDFeatureFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(SHORTS_HUD_FEATURE_FLAG),
)

internal val reelFeedbackFingerprint = legacyFingerprint(
    name = "reelFeedbackFingerprint",
    returnType = "V",
    literals = listOf(reelFeedbackLike, reelFeedbackPause, reelFeedbackPlay),
)

internal val shortsButtonFingerprint = legacyFingerprint(
    name = "shortsButtonFingerprint",
    returnType = "V",
    literals = listOf(
        reelDynRemix,
        reelDynShare,
        reelRightDislikeIcon,
        reelRightLikeIcon,
        rightComment
    ),
)

/**
 * The method by which patches are applied is different between the minimum supported version and the maximum supported version.
 * There are two classes where R.id.badge_label[badgeLabel] is used,
 * but due to the structure of ReVanced Patcher, the patch is applied to the method found first.
 */
internal val shortsPaidPromotionFingerprint = legacyFingerprint(
    name = "shortsPaidPromotionFingerprint",
    literals = listOf(badgeLabel),
)

internal val shortsPausedHeaderFingerprint = legacyFingerprint(
    name = "shortsPausedHeaderFingerprint",
    returnType = "Landroid/view/View;",
    opcodes = listOf(
        Opcode.IF_NEZ,
        Opcode.IGET_OBJECT,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    strings = listOf("r_pfcv")
)

internal val shortsPivotLegacyFingerprint = legacyFingerprint(
    name = "shortsPivotLegacyFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("Z", "Z", "L"),
    literals = listOf(reelForcedMuteButton),
)

internal val shortsSubscriptionsTabletFingerprint = legacyFingerprint(
    name = "shortsSubscriptionsTabletFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("L", "L", "Z"),
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.IGET,
        Opcode.IF_EQZ
    )
)

internal val shortsSubscriptionsTabletParentFingerprint = legacyFingerprint(
    name = "shortsSubscriptionsTabletParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(reelPlayerFooter),
)

internal val shortsTimeStampConstructorFingerprint = legacyFingerprint(
    name = "shortsTimeStampConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(reelVodTimeStampsContainer),
)

internal val shortsTimeStampMetaPanelFingerprint = legacyFingerprint(
    name = "shortsTimeStampMetaPanelFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(metaPanel),
)

internal val shortsTimeStampPrimaryFingerprint = legacyFingerprint(
    name = "shortsTimeStampPrimaryFingerprint",
    returnType = "I",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I"),
    literals = listOf(45627350L, 45638282L, 10002L),
)

internal val shortsTimeStampSecondaryFingerprint = legacyFingerprint(
    name = "shortsTimeStampSecondaryFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(45638187L),
)

internal val shortsToolBarFingerprint = legacyFingerprint(
    name = "shortsToolBarFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(Opcode.IPUT_BOOLEAN),
    strings = listOf("Null topBarButtons"),
    customFingerprint = { method, _ ->
        method.parameterTypes.firstOrNull() == "Z"
    }
)

internal const val FULLSCREEN_FEATURE_FLAG = 45398938L

internal val shortsFullscreenFeatureFingerprint = legacyFingerprint(
    name = "shortsFullscreenFeatureFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(FULLSCREEN_FEATURE_FLAG),
)

internal val shortsExperimentalPlayerFeatureFlagFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    parameters()
    literal {
        45677719L
    }
}

internal val renderNextUIFeatureFlagFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    parameters()
    literal {
        45649743L
    }
}
