package app.revanced.patches.youtube.feed.components

import app.revanced.patches.youtube.utils.resourceid.bar
import app.revanced.patches.youtube.utils.resourceid.barContainerHeight
import app.revanced.patches.youtube.utils.resourceid.captionToggleContainer
import app.revanced.patches.youtube.utils.resourceid.channelListSubMenu
import app.revanced.patches.youtube.utils.resourceid.contentPill
import app.revanced.patches.youtube.utils.resourceid.drawerResults
import app.revanced.patches.youtube.utils.resourceid.expandButtonDown
import app.revanced.patches.youtube.utils.resourceid.filterBarHeight
import app.revanced.patches.youtube.utils.resourceid.horizontalCardList
import app.revanced.patches.youtube.utils.resourceid.relatedChipCloudMargin
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val breakingNewsFingerprint = legacyFingerprint(
    name = "breakingNewsFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(horizontalCardList),
)

internal val captionsButtonFingerprint = legacyFingerprint(
    name = "captionsButtonFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
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

internal val elementParserFingerprint = legacyFingerprint(
    name = "elementParserFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L", "[B", "L", "L"),
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.RETURN_OBJECT
    )
)

internal val elementParserParentFingerprint = legacyFingerprint(
    name = "elementParserParentFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("Element tree missing id in debug mode.")
)

internal val engagementPanelUpdateFingerprint = legacyFingerprint(
    name = "engagementPanelUpdateFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("L", "Z"),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>().toString() == "Ljava/util/ArrayDeque;->pop()Ljava/lang/Object;"
        } >= 0
    }
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

internal val linearLayoutManagerItemCountsFingerprint = legacyFingerprint(
    name = "linearLayoutManagerItemCountsFingerprint",
    returnType = "I",
    accessFlags = AccessFlags.FINAL.value,
    parameters = listOf("L", "L", "L", "Z"),
    opcodes = listOf(
        Opcode.IF_NEZ,
        Opcode.IF_LEZ,
        Opcode.INVOKE_VIRTUAL,
    ),
    customFingerprint = { method, _ ->
        method.definingClass == "Landroid/support/v7/widget/LinearLayoutManager;"
    }
)

internal val relatedChipCloudFingerprint = legacyFingerprint(
    name = "relatedChipCloudFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
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

internal val showMoreButtonFingerprint = legacyFingerprint(
    name = "showMoreButtonFingerprint",
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT
    ),
    literals = listOf(expandButtonDown),
)


