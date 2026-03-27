package app.morphe.patches.shared.comments

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

internal val engagementPanelIdFingerprint = legacyFingerprint(
    name = "engagementPanelIdFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IGET,
        Opcode.CONST_16,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
    ),
    literals = listOf(18L),
)

internal val engagementPanelRecyclerViewFingerprint = legacyFingerprint(
    name = "engagementPanelRecyclerViewFingerprint",
    returnType = "V",
    literals = listOf(49399797L),
    customFingerprint = { method, classDef ->
        !AccessFlags.STATIC.isSet(method.accessFlags) &&
                classDef.fields.find { field -> field.type == "Lcom/google/android/libraries/youtube/rendering/ui/widget/loadingframe/LoadingFrameLayout;" } != null &&
                classDef.fields.find { field -> field.type == "Lj\$/util/Optional;" } != null &&
                indexOfRecyclerViewInstruction(method) >= 0 &&
                indexOfIfPresentInstruction(method) >= 0

    },
)

internal val recyclerViewOptionalFingerprint = legacyFingerprint(
    name = "recyclerViewOptionalFingerprint",
    returnType = "Lj\$/util/Optional;",
    parameters = emptyList(),
    customFingerprint = { method, _ ->
        indexOfRecyclerViewInstruction(method) >= 0
    }
)

internal fun indexOfRecyclerViewInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.CHECK_CAST &&
                getReference<TypeReference>()?.type == "Landroid/support/v7/widget/RecyclerView;"
    }

internal fun indexOfIfPresentInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "ifPresent"
    }

internal val engagementPanelTitleFingerprint = legacyFingerprint(
    name = "engagementPanelTitleFingerprint",
    literals = listOf(informationButton, modernTitle, title)
)

internal val engagementPanelTitleParentFingerprint = legacyFingerprint(
    name = "engagementPanelTitleParentFingerprint",
    strings = listOf("[EngagementPanelTitleHeader] Cannot remove action buttons from header as the child count is out of sync. Buttons to remove exceed current header child count.")
)

internal val recyclerViewSmoothScrollToPositionFingerprint = legacyFingerprint(
    name = "recyclerViewSmoothScrollToPositionFingerprint",
    returnType = "V",
    parameters = listOf("I"),
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("Cannot smooth scroll without a LayoutManager set. Call setLayoutManager with a non-null argument.")
)
