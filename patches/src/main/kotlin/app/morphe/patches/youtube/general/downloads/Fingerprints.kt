/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.downloads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import app.morphe.util.parametersEqual
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

internal object FeedBottomSheetFlyoutFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        parameters = listOf("Landroid/os/Bundle;"),
        strings = listOf(
            "BaseBottomSheetDialogFragment.useNewUi",
            "BaseBottomSheetDialogFragment.peekHeightEnabled",
            "BaseBottomSheetDialogFragment.largeFormWidthDp",
        ),
    ),
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Landroid/app/Dialog;",
    parameters = listOf("Landroid/os/Bundle;"),
)

internal object FeedFlyoutBufferObjectFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    strings = listOf(
        "com.google.android.libraries.youtube.rendering.elements.sender_view",
        "com.google.android.libraries.youtube.innertube.endpoint.tag",
        "com.google.android.libraries.youtube.innertube.bundle",
        "com.google.android.libraries.youtube.logging.interaction_logger",
    ),
)

internal object FullHistoryFlyoutBufferObjectFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "innertube_menu_anchor_model"),
        resourceLiteral(ResourceType.ID, "innertube_menu_anchor_tag"),
        opcode(Opcode.MOVE_RESULT_OBJECT),
        resourceLiteral(ResourceType.ID, "innertube_menu_anchor_interaction_logger"),
    ),
    custom = { method, _ -> method.name == "onClick" },
)

internal object FeedFlyoutButtonsInitializerFingerprint : Fingerprint(
    parameters = listOf("L"),
    filters = listOf(
        opcode(Opcode.INVOKE_STATIC),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            returnType = "Ljava/lang/CharSequence;",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.CONST_4),
        opcode(Opcode.IF_NEZ),
        opcode(Opcode.AND_INT_2ADDR, location = MatchAfterWithin(3)),
        fieldAccess(opcode = Opcode.IGET, type = "I", location = MatchAfterWithin(4)),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            parameters = listOf("I"),
            location = MatchAfterImmediately(),
        ),
        methodCall(opcode = Opcode.INVOKE_DIRECT, name = "<init>"),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, type = "Ljava/lang/Runnable;"),
    ),
    strings = listOf(
        "ElementTransformer cannot be null",
        "Text missing for BottomSheetMenuItem.",
        "Text missing for BottomSheetMenuItem with iconType: ",
    ),
)

internal object FeedFlyoutButtonsInitializerOnItemClickFingerprint : Fingerprint(
    classFingerprint = FeedFlyoutButtonsInitializerFingerprint,
    name = "onItemClick",
)

internal object InteractiveStickerRendererGetEditViewFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/view/View;",
    parameters = emptyList(),
    strings = listOf("getEditView called without setting interactiveStickerRenderer"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "[B"),
    ),
)

internal object FlyoutMenuItemMessageFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("Ljava/lang/String;", "Lcom/google/protobuf/MessageLite;"),
    filters = listOf(
        literal(42357),
        opcode(Opcode.INSTANCE_OF, location = MatchAfterWithin(10)),
        string("downloads_page_downloads_item_section_identifier"),
    ),
)

internal object SingularGeneratedExtensionFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.CONSTRUCTOR, AccessFlags.STATIC),
    filters = listOf(
        methodCall(name = "registerDefaultInstance"),
        fieldAccess(opcode = Opcode.SGET_OBJECT, type = "L", location = MatchAfterWithin(2)),
        string(""),
        literal(125983101),
        methodCall(name = "newSingularGeneratedExtension"),
    ),
)

private val ENDS_WITH_PARAMETER_LIST = listOf(
    "Lcom/google/android/apps/youtube/app/offline/ui/OfflineArrowView;",
    "I",
    "Landroid/view/View${'$'}OnClickListener;"
)

internal val accessibilityOfflineButtonSyncFingerprint = legacyFingerprint(
    name = "accessibilityOfflineButtonSyncFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    customFingerprint = custom@{ method, _ ->
        if (!MethodUtil.isConstructor(method)) {
            return@custom false
        }
        val parameterTypes = method.parameterTypes
        val parameterSize = parameterTypes.size
        if (parameterSize < 6) {
            return@custom false
        }

        val endsWithMethodParameterList = parameterTypes.slice(parameterSize - 3..<parameterSize)
        parametersEqual(ENDS_WITH_PARAMETER_LIST, endsWithMethodParameterList)
    }
)

internal val downloadPlaylistButtonOnClickFingerprint = legacyFingerprint(
    name = "downloadPlaylistButtonOnClickFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    opcodes = listOf(Opcode.INVOKE_VIRTUAL_RANGE),
    customFingerprint = { method, _ ->
        indexOfPlaylistDownloadActionInvokeInstruction(method) >= 0
    }
)

internal fun indexOfPlaylistDownloadActionInvokeInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.parameterTypes ==
                listOf(
                    "Ljava/lang/String;",
                    "Lcom/google/android/apps/youtube/app/offline/ui/OfflineArrowView;",
                    "I",
                    "Landroid/view/View${'$'}OnClickListener;"
                )
    }

internal val offlinePlaylistEndpointFingerprint = legacyFingerprint(
    name = "offlinePlaylistEndpointFingerprint",
    returnType = "V",
    strings = listOf("Object is not an offlineable playlist: ")
)

internal val offlineVideoEndpointFingerprint = legacyFingerprint(
    name = "offlineVideoEndpointFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf(
        "Ljava/util/Map;",
        "L",
        "Ljava/lang/String", // VideoId
        "L"
    ),
    strings = listOf("Object is not an offlineable video: ")
)

internal val setPlaylistDownloadButtonVisibilityFingerprint = legacyFingerprint(
    name = "setPlaylistDownloadButtonVisibilityFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
        Opcode.IGET,
        Opcode.CONST_4
    )
)
