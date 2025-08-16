package app.revanced.patches.shared.scrolltop

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.shared.extension.Constants.PATCHES_PATH
import app.revanced.patches.shared.mapping.ResourceType.ID
import app.revanced.patches.shared.mapping.getResourceId
import app.revanced.patches.shared.mapping.resourceMappingPatch
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.findFreeRegister
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodCall
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.injectLiteralInstructionViewCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

var modernTitle = -1L
    private set
var title = -1L
    private set

private val commentsScrollTopResourcePatch = resourcePatch(
    description = "commentsScrollTopResourcePatch"
) {
    dependsOn(resourceMappingPatch)

    execute {
        modernTitle = getResourceId(ID, "modern_title")
        title = getResourceId(ID, "title")
    }
}

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/CommentsScrollTopPatch;"

val commentsScrollTopPatch = bytecodePatch(
    description = "commentsScrollTopPatch"
) {
    dependsOn(commentsScrollTopResourcePatch)

    execute {
        // Method to find the engagement panel id.
        val (engagementPanelIdMethodCall, engagementPanelMessageClass) =
            with (engagementPanelIdFingerprint.methodOrThrow()) {
                Pair(methodCall(), parameterTypes.first().toString())
            }

        // Method that finds the RecyclerView to which comments will be bound.
        val recyclerViewOptionalMethodCall = recyclerViewOptionalFingerprint
            .methodOrThrow(engagementPanelRecyclerViewFingerprint)
            .methodCall()

        engagementPanelRecyclerViewFingerprint.matchOrThrow().let { result ->
            result.method.apply {
                val setRecyclerViewMethodName = "patch_setRecyclerView"
                val insertIndex = indexOfIfPresentInstruction(this) + 1

                // Find the index of the class required to get the engagement panel id.
                val engagementPanelMessageIndex = indexOfFirstInstructionOrThrow(insertIndex) {
                    getReference<MethodReference>()?.parameterTypes?.firstOrNull() == engagementPanelMessageClass
                }
                val engagementPanelMessageRegister = getInstruction<FiveRegisterInstruction>(engagementPanelMessageIndex).let { instruction ->
                    if (getInstruction(engagementPanelMessageIndex).opcode == Opcode.INVOKE_STATIC)
                        instruction.registerC
                    else // YouTube Music 6.20.51
                        instruction.registerD
                }
                val freeRegister = findFreeRegister(insertIndex, false)

                addInstructionsAtControlFlowLabel(
                    insertIndex, """
                        move-object/from16 v$freeRegister, p0
                        invoke-direct { v$freeRegister, v$engagementPanelMessageRegister }, $definingClass->$setRecyclerViewMethodName($engagementPanelMessageClass)V
                        """
                )

                result.classDef.methods.add(
                    ImmutableMethod(
                        result.classDef.type,
                        setRecyclerViewMethodName,
                        listOf(
                            ImmutableMethodParameter(
                                engagementPanelMessageClass,
                                annotations,
                                "engagementPanelMessageClass"
                            )
                        ),
                        "V",
                        AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                        annotations,
                        null,
                        MutableMethodImplementation(4),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->isCommentsScrollTopEnabled()Z
                                    move-result v0
                                    if-eqz v0, :ignore
                                    
                                    # Get engagement panel id.
                                    invoke-static { p1 }, $engagementPanelIdMethodCall
                                    move-result-object v0
                                    
                                    # Check if engagement panel id is not null.
                                    if-eqz v0, :ignore

                                    const-string v1, "comment"
                                    invoke-virtual { v0, v1 }, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                                    move-result v0
                                    
                                    # Check if engagement panel id is a comment.
                                    if-eqz v0, :ignore
                                    
                                    invoke-virtual { p0 }, $recyclerViewOptionalMethodCall
                                    move-result-object v0
                                    invoke-virtual { v0 }, Lj${'$'}/util/Optional;->isPresent()Z
                                    move-result v1
                                    
                                    # Check if recycler view is not null.
                                    if-eqz v1, :ignore
                                    invoke-virtual { v0 }, Lj${'$'}/util/Optional;->get()Ljava/lang/Object;
                                    move-result-object v0
                                    check-cast v0, Landroid/support/v7/widget/RecyclerView;
                                    invoke-static { v0 }, $EXTENSION_CLASS_DESCRIPTOR->onCommentsCreate(Landroid/support/v7/widget/RecyclerView;)V
                                    
                                    :ignore
                                    return-void
                                    """,
                        )
                    }
                )
            }
        }

        arrayOf(
            modernTitle,
            title
        ).forEach { literal ->
            engagementPanelTitleFingerprint
                .methodOrThrow(engagementPanelTitleParentFingerprint)
                .injectLiteralInstructionViewCall(
                    literal,
                    "invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $EXTENSION_CLASS_DESCRIPTOR->setContentHeader(Landroid/view/View;)V"
                )
        }

        findMethodOrThrow(EXTENSION_CLASS_DESCRIPTOR) {
            name == "smoothScrollToPosition"
        }.addInstruction(
            0,
            "invoke-virtual {p0, p1}, ${recyclerViewSmoothScrollToPositionFingerprint.methodCall()}"
        )
    }
}

