package app.morphe.patches.youtube.feed.components

import app.morphe.patches.youtube.utils.resourceid.*
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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
    ),
    customFingerprint = { method, _ ->
        indexOfBufferParserInstruction(method) >= 0
    }
)

internal fun indexOfBufferParserInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        reference?.parameterTypes?.firstOrNull() == "[B" &&
                reference.returnType.startsWith("L")
    }

internal val elementParserParentFingerprint = legacyFingerprint(
    name = "elementParserParentFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("Element tree missing id in debug mode.")
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


