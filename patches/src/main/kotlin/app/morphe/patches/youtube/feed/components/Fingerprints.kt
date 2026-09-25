/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.feed.components

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.checkCast
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patches.youtube.utils.resourceid.bar
import app.morphe.patches.youtube.utils.resourceid.barContainerHeight
import app.morphe.patches.youtube.utils.resourceid.captionToggleContainer
import app.morphe.patches.youtube.utils.resourceid.channelListSubMenu
import app.morphe.patches.youtube.utils.resourceid.contentPill
import app.morphe.patches.youtube.utils.resourceid.drawerResults
import app.morphe.patches.youtube.utils.resourceid.expandButtonDown
import app.morphe.patches.youtube.utils.resourceid.filterBarHeight
import app.morphe.patches.youtube.utils.resourceid.horizontalCardList
import app.morphe.patches.youtube.utils.resourceid.relatedChipCloudMargin
import app.morphe.util.containsLiteralInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val breakingNewsFingerprint = "breakingNewsFingerprint" to Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        literal(horizontalCardList),
    ),
)

internal val captionsButtonFingerprint = "captionsButtonFingerprint" to Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        literal(captionToggleContainer),
    ),
)

internal val captionsButtonSyntheticFingerprint = "captionsButtonSyntheticFingerprint" to Fingerprint(
    returnType = "Landroid/view/View;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.BRIDGE, AccessFlags.SYNTHETIC),
    parameters = listOf("Landroid/content/Context;"),
    filters = listOf(
        literal(captionToggleContainer),
    ),
)

/**
 * Matches the 21.13+ feed overlay state update that guards the captions container.
 */
internal object ModernCaptionsButtonFingerprint : Fingerprint(
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        opcode(Opcode.IF_EQZ),
        literal(captionToggleContainer, location = MatchAfterWithin(4)),
    ),
)

/**
 * Matches the 21.13+ feed overlay setup that initializes the captions' container.
 */
internal object ModernCaptionsButtonSyntheticFingerprint : Fingerprint(
    filters = listOf(
        literal(captionToggleContainer),
        checkCast(
            "Landroid/view/ViewGroup;",
            location = MatchAfterWithin(10),
        ),
    ),
)

internal val channelListSubMenuFingerprint = "channelListSubMenuFingerprint" to Fingerprint(
    filters = listOf(
        literal(channelListSubMenu),
    ),
)

internal val channelListSubMenuTabletFingerprint = "channelListSubMenuTabletFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    filters = listOf(
        literal(drawerResults),
    ),
)

internal val channelListSubMenuTabletSyntheticFingerprint = "channelListSubMenuTabletSyntheticFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.SYNTHETIC),
    strings = listOf("is_horizontal_drawer_context")
)

internal val channelTabBuilderFingerprint = "channelTabBuilderFingerprint" to Fingerprint(
    returnType = "Landroid/view/View;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/CharSequence;", "Ljava/lang/CharSequence;", "Z", "L")
)

internal val channelTabRendererFingerprint = "channelTabRendererFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "Ljava/util/List;", "I"),
    filters = listOf(
        opcode(Opcode.IF_EQ),
        anyInstruction(
            methodCall(
                opcode = Opcode.INVOKE_INTERFACE,
                returnType = "V",
                parameters = listOf("I", "Z", "Z", "Z")
            ),
            methodCall( // ~21.25
                opcode = Opcode.INVOKE_INTERFACE,
                returnType = "V",
                parameters = listOf("I", "Z", "Z")
            ),
            methodCall( // ~21.16 and older
                opcode = Opcode.INVOKE_INTERFACE,
                returnType = "V",
                parameters = listOf("I")
            ),
            location = MatchAfterWithin(3)
        ),
        opcode(
            Opcode.RETURN_VOID,
            MatchAfterImmediately()
        )
    ),
    strings = listOf("TabRenderer.content contains SectionListRenderer but the tab does not have a section list controller.")
)

internal val contentPillFingerprint = "contentPillFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "Z"),
    filters = listOf(
        literal(contentPill),
    ),
)

internal object ParseElementFromBufferFingerprint : Fingerprint(
    parameters = listOf("L", "L", "[B", "L", "L"),
    filters = listOf(
        opcode(Opcode.IGET_OBJECT),
        // IGET_BOOLEAN // 20.07+
        opcode(Opcode.INVOKE_INTERFACE, location = MatchAfterWithin(1)),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        string("Failed to parse Element", StringComparisonType.STARTS_WITH),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("L"),
            returnType = "L"
        ),
        opcode(Opcode.RETURN_OBJECT, location = MatchAfterWithin(4))
    )
)

internal val filterBarHeightFingerprint = "filterBarHeightFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IPUT,
    ),
    custom = { method, _ -> method.containsLiteralInstruction(filterBarHeight) },
)

internal val latestVideosButtonFingerprint = "latestVideosButtonFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("L", "Z"),
    filters = listOf(
        literal(bar),
    ),
)

internal val relatedChipCloudFingerprint = "relatedChipCloudFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        literal(relatedChipCloudMargin),
    ),
)

internal val searchResultsChipBarFingerprint = "searchResultsChipBarFingerprint" to Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
    ),
    custom = { method, _ -> method.containsLiteralInstruction(barContainerHeight) },
)

internal val showMoreButtonParentFingerprint = "showMoreButtonParentFingerprint" to Fingerprint(
    returnType = "V",
    filters = listOf(
        literal(expandButtonDown),
    ),
)

internal val showMoreButtonFingerprint = "showMoreButtonFingerprint" to Fingerprint(
    returnType = "Landroid/view/View;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
)
