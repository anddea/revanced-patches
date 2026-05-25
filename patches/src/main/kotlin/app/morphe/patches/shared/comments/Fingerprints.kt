package app.morphe.patches.shared.comments

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.util.containsLiteralInstruction
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

internal val engagementPanelIdFingerprint = Fingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET,
        Opcode.CONST_16,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
    ),
    custom = { method, _ ->
        method.containsLiteralInstruction(18L)
    },
)

internal val engagementPanelRecyclerViewFingerprint = Fingerprint(
    returnType = "V",
    custom = { method, classDef ->
        !AccessFlags.STATIC.isSet(method.accessFlags) &&
                method.containsLiteralInstruction(49399797L) &&
                classDef.fields.find { field -> field.type == "Lcom/google/android/libraries/youtube/rendering/ui/widget/loadingframe/LoadingFrameLayout;" } != null &&
                classDef.fields.find { field -> field.type == "Lj\$/util/Optional;" } != null &&
                indexOfRecyclerViewInstruction(method) >= 0 &&
                indexOfIfPresentInstruction(method) >= 0

    },
)

internal val recyclerViewOptionalFingerprint = Fingerprint(
    returnType = "Lj\$/util/Optional;",
    parameters = emptyList(),
    custom = { method, _ ->
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

internal val engagementPanelTitleFingerprint = Fingerprint(
    custom = { method, _ ->
        method.containsLiteralInstruction(informationButton) &&
                method.containsLiteralInstruction(modernTitle) &&
                method.containsLiteralInstruction(title)
    }
)

internal val engagementPanelTitleParentFingerprint = Fingerprint(
    strings = listOf("[EngagementPanelTitleHeader] Cannot remove action buttons from header as the child count is out of sync. Buttons to remove exceed current header child count.")
)

internal val recyclerViewSmoothScrollToPositionFingerprint = Fingerprint(
    returnType = "V",
    parameters = listOf("I"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    strings = listOf("Cannot smooth scroll without a LayoutManager set. Call setLayoutManager with a non-null argument.")
)
