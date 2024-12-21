package app.revanced.patches.youtube.shorts.components

import app.revanced.patches.youtube.utils.resourceid.badgeLabel
import app.revanced.patches.youtube.utils.resourceid.metaPanel
import app.revanced.patches.youtube.utils.resourceid.reelDynRemix
import app.revanced.patches.youtube.utils.resourceid.reelDynShare
import app.revanced.patches.youtube.utils.resourceid.reelFeedbackLike
import app.revanced.patches.youtube.utils.resourceid.reelFeedbackPause
import app.revanced.patches.youtube.utils.resourceid.reelFeedbackPlay
import app.revanced.patches.youtube.utils.resourceid.reelForcedMuteButton
import app.revanced.patches.youtube.utils.resourceid.reelPlayerFooter
import app.revanced.patches.youtube.utils.resourceid.reelRightDislikeIcon
import app.revanced.patches.youtube.utils.resourceid.reelRightLikeIcon
import app.revanced.patches.youtube.utils.resourceid.reelVodTimeStampsContainer
import app.revanced.patches.youtube.utils.resourceid.rightComment
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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

