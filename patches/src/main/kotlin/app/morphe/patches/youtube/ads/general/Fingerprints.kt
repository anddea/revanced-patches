/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.ads.general

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import app.morphe.patcher.util.getReference
import app.morphe.patches.youtube.utils.resourceid.badgeLabel
import app.morphe.patches.youtube.utils.resourceid.fullScreenEngagementAdContainer
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.literal
import app.morphe.patches.shared.ads.playerBytesAdLayoutFingerprint
import app.morphe.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val compactYpcOfferModuleViewFingerprint = Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    parameters = listOf("I", "I"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.ADD_INT_2ADDR,
        Opcode.ADD_INT_2ADDR,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    custom = { method, _ ->
        method.definingClass.endsWith("/CompactYpcOfferModuleView;") &&
                method.name == "onMeasure"
    }
)

internal val fullScreenEngagementAdContainerFingerprint = Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    filters = listOf(literal(fullScreenEngagementAdContainer)),
    custom = { method, _ ->
        indexOfAddListInstruction(method) >= 0
    }
)

internal fun indexOfAddListInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "add"
    }

/**
 * The method by which patches are applied is different between the minimum supported version and the maximum supported version.
 * There are two classes where R.id.badge_label[badgeLabel] is used,
 * but due to the structure of ReVanced Patcher, the patch is applied to the method found first.
 */
internal val shortsPaidPromotionFingerprint = Fingerprint(
    filters = listOf(literal(badgeLabel)),
)

internal object PlayerOverlayTimelyShelfFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        string("player_overlay_timely_shelf"),
        string("innertube_cue_range"),
        string("Null id"),
        string("Null onExitActions")
    )
)

// YouTube 21.39: the timely shelf handler was split up; "player_overlay_timely_shelf" is now
// compared inside a switch-lambda (Object)V that also contains "menu_item_stable_volume".
internal val playerOverlayTimelyShelfSwitchFingerprint = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf(
        string("player_overlay_timely_shelf", StringComparisonType.CONTAINS),
        string("menu_item_stable_volume", StringComparisonType.CONTAINS)
    ),
)

/**
 * Supplies the future the player waits on before it starts a video that has a
 * player bytes ad slot. The future is completed only once the ad slot is scheduled.
 */
internal object PlayerBytesAdSchedulingFingerprint : Fingerprint(
    classFingerprint = playerBytesAdLayoutFingerprint.second,
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Lj$/util/Optional;",
    parameters = listOf("L", "L", "Ljava/lang/String;"),
)

/**
 * Handles the activated triggers of the ad slots, such as the triggers that enter a slot
 * and the triggers that expire a slot.
 */
internal object LoadVideoAdsFingerprint : Fingerprint(
    strings = listOf(
        "TriggerBundle doesn't have the required metadata specified by the trigger ",
        "Ping migration no associated ping bindings for activated trigger: ",
    ),
    filters = listOf(
        // The triggers are handled by their category.
        fieldAccess(opcode = Opcode.IGET, type = "I"),
        methodCall(
            smali = "Landroid/util/SparseArray;->get(I)Ljava/lang/Object;",
            location = MatchAfterImmediately()
        )
    )
)
