/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
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
import app.morphe.patcher.StringComparisonType
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
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val breakingNewsFingerprint = legacyFingerprint(
    name = "breakingNewsFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(horizontalCardList),
)

internal val captionsButtonFingerprint = legacyFingerprint(
    name = "captionsButtonFingerprint",
    returnType = "V",
    parameters = emptyList(),
    literals = listOf(captionToggleContainer),
)

internal val captionsButtonSyntheticFingerprint = legacyFingerprint(
    name = "captionsButtonSyntheticFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.BRIDGE or AccessFlags.SYNTHETIC,
    parameters = listOf("Landroid/content/Context;"),
    literals = listOf(captionToggleContainer),
)

internal val channelListSubMenuFingerprint = legacyFingerprint(
    name = "channelListSubMenuFingerprint",
    literals = listOf(channelListSubMenu),
)

internal val channelListSubMenuTabletFingerprint = legacyFingerprint(
    name = "channelListSubMenuTabletFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(drawerResults),
)

internal val channelListSubMenuTabletSyntheticFingerprint = legacyFingerprint(
    name = "channelListSubMenuTabletSyntheticFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.SYNTHETIC,
    strings = listOf("is_horizontal_drawer_context")
)

internal val channelTabBuilderFingerprint = legacyFingerprint(
    name = "channelTabBuilderFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/CharSequence;", "Ljava/lang/CharSequence;", "Z", "L")
)

internal val channelTabRendererFingerprint = legacyFingerprint(
    name = "channelTabRendererFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Ljava/util/List;", "I"),
    strings = listOf("TabRenderer.content contains SectionListRenderer but the tab does not have a section list controller.")
)

internal val contentPillFingerprint = legacyFingerprint(
    name = "contentPillFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Z"),
    literals = listOf(contentPill),
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

internal val filterBarHeightFingerprint = legacyFingerprint(
    name = "filterBarHeightFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IPUT
    ),
    literals = listOf(filterBarHeight),
)

internal val latestVideosButtonFingerprint = legacyFingerprint(
    name = "latestVideosButtonFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Z"),
    literals = listOf(bar),
)

internal val relatedChipCloudFingerprint = legacyFingerprint(
    name = "relatedChipCloudFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(relatedChipCloudMargin),
)

internal val searchResultsChipBarFingerprint = legacyFingerprint(
    name = "searchResultsChipBarFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    literals = listOf(barContainerHeight),
)

internal val showMoreButtonParentFingerprint = legacyFingerprint(
    name = "showMoreButtonParentFingerprint",
    returnType = "V",
    literals = listOf(expandButtonDown),
)

internal val showMoreButtonFingerprint = legacyFingerprint(
    name = "showMoreButtonFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
)
