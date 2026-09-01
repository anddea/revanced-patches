/*
 * Copyright (C) 2024-2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

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
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.parametersEqual
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.util.MethodUtil

internal object FeedBottomSheetFlyoutFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        filters = listOf(
            resourceLiteral(ResourceType.DRAWABLE, "sheet_handle"),
        ),
    ),
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Landroid/app/Dialog;",
    parameters = listOf("Landroid/os/Bundle;"),
)

internal object ModernFeedBottomSheetFlyoutFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        parameters = listOf("Landroid/os/Bundle;"),
        filters = listOf(
            string("BaseBottomSheetDialogFragment.useNewUi"),
            string("BaseBottomSheetDialogFragment.peekHeightEnabled"),
            string("BaseBottomSheetDialogFragment.largeFormWidthDp"),
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

internal object FlyoutBufferClassFingerprint : Fingerprint(
    custom = { method, classDef ->
        AccessFlags.STATIC.isSet(method.accessFlags) &&
                method.returnType == "V" &&
                method.parameterTypes == listOf("Ljava/lang/Object;") &&
                classDef.fields.count { it.type == "[B" } == 1 &&
                method.indexOfFirstInstruction {
                    opcode == Opcode.INSTANCE_OF &&
                            getReference<TypeReference>()?.type == classDef.type
                } >= 0
    }
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
    $$"Landroid/view/View$OnClickListener;"
)

internal val accessibilityOfflineButtonSyncFingerprint = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    custom = custom@{ method, _ ->
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
    },
)

internal val downloadPlaylistButtonOnClickFingerprint = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(Opcode.INVOKE_VIRTUAL_RANGE),
    custom = { method, _ ->
        indexOfPlaylistDownloadActionInvokeInstruction(method) >= 0
    },
)

internal fun indexOfPlaylistDownloadActionInvokeInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.parameterTypes ==
                listOf(
                    "Ljava/lang/String;",
                    "Lcom/google/android/apps/youtube/app/offline/ui/OfflineArrowView;",
                    "I",
                    $$"Landroid/view/View$OnClickListener;"
                )
    }

internal val offlinePlaylistEndpointFingerprint = Fingerprint(
    returnType = "V",
    strings = listOf("Object is not an offlineable playlist: "),
)

internal val offlineVideoEndpointFingerprint = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Ljava/util/Map;",
        "L",
        "Ljava/lang/String;", // VideoId
        "L"
    ),
    strings = listOf("Object is not an offlineable video: "),
)

internal val setPlaylistDownloadButtonVisibilityFingerprint = Fingerprint(
    returnType = "V",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
        Opcode.IGET,
        Opcode.CONST_4
    ),
)
