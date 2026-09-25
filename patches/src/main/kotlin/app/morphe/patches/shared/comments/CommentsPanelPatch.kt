/*
 * Copyright (C) 2026 anddea
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

package app.morphe.patches.shared.comments

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.extension.Constants.PATCHES_PATH
import app.morphe.patches.shared.mapping.ResourceType.ID
import app.morphe.patches.shared.mapping.getResourceId
import app.morphe.patches.shared.mapping.resourceMappingPatch
import app.morphe.util.REGISTER_TEMPLATE_REPLACEMENT
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.methodCall
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.injectLiteralInstructionViewCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

var informationButton = -1L
    private set
var modernTitle = -1L
    private set
var title = -1L
    private set

private val commentsPanelResourcePatch = resourcePatch(
    description = "commentsPanelResourcePatch"
) {
    dependsOn(resourceMappingPatch)

    execute {
        informationButton = getResourceId(ID, "information_button")
        modernTitle = getResourceId(ID, "modern_title")
        title = getResourceId(ID, "title")
    }
}

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/CommentsPanelPatch;"

val commentsPanelPatch = bytecodePatch(
    description = "commentsPanelPatch"
) {
    dependsOn(commentsPanelResourcePatch)

    execute {
        // Method to find the engagement panel id.
        val (engagementPanelIdMethodCall, engagementPanelMessageClass) =
            with(engagementPanelIdFingerprint.method) {
                Pair(methodCall(), parameterTypes.first().toString())
            }

        // Method that finds the RecyclerView to which comments will be bound.
        // Resolve from the matched engagement-panel class. A second global fingerprint match can
        // select a sibling class in YouTube Music 9.15.
        val recyclerViewOptionalMethodCall = engagementPanelRecyclerViewFingerprint.classDef.methods
            .first { method ->
                method.returnType == "Lj$/util/Optional;" &&
                        method.parameters.isEmpty() &&
                        indexOfRecyclerViewInstruction(method) >= 0
            }
            .methodCall()

        engagementPanelRecyclerViewFingerprint.let { result ->
            result.method.apply {
                val setRecyclerViewMethodName = "patch_setRecyclerView"
                val engagementPanelMessageRegister = parameterTypes
                    .indexOfFirst { it.toString() == engagementPanelMessageClass }
                    .takeIf { it >= 0 }
                    ?.let { "p${it + 1}" }

                val (insertIndex, setRecyclerViewInstructions) =
                    if (engagementPanelMessageRegister == "p1") {
                        val recyclerViewCheckCastIndex = indexOfRecyclerViewInstruction(this)
                        val recyclerViewOptionalPresentIndex = indexOfFirstInstructionReversedOrThrow(
                            recyclerViewCheckCastIndex
                        ) {
                            opcode == Opcode.INVOKE_VIRTUAL &&
                                    getReference<MethodReference>()?.name == "isPresent"
                        }
                        val insertIndex = indexOfFirstInstructionOrThrow(recyclerViewOptionalPresentIndex) {
                            opcode == Opcode.IF_EQZ
                        }

                        insertIndex to
                                "invoke-direct/range { p0 .. p1 }, $definingClass->$setRecyclerViewMethodName($engagementPanelMessageClass)V"
                    } else {
                        val insertIndex = indexOfIfPresentInstruction(this) + 1

                        val messageRegister = engagementPanelMessageRegister ?: run {
                            // Older versions keep the engagement panel message in a local register.
                            val engagementPanelMessageIndex = indexOfFirstInstructionOrThrow(insertIndex) {
                                getReference<MethodReference>()?.parameterTypes?.firstOrNull() == engagementPanelMessageClass
                            }
                            getInstruction<FiveRegisterInstruction>(engagementPanelMessageIndex).let { instruction ->
                                val register = if (getInstruction(engagementPanelMessageIndex).opcode == Opcode.INVOKE_STATIC)
                                    instruction.registerC
                                else // YouTube Music 6.20.51
                                    instruction.registerD

                                "v$register"
                            }
                        }

                        val freeRegisters = getFreeRegisterProvider(insertIndex, 2)
                        val thisRegister = freeRegisters.getFreeRegister()
                        val engagementPanelMessageFreeRegister = freeRegisters.getFreeRegister()

                        insertIndex to """
                            move-object/from16 v$thisRegister, p0
                            move-object/from16 v$engagementPanelMessageFreeRegister, $messageRegister
                            invoke-direct { v$thisRegister, v$engagementPanelMessageFreeRegister }, $definingClass->$setRecyclerViewMethodName($engagementPanelMessageClass)V
                            """
                    }

                addInstructionsAtControlFlowLabel(
                    insertIndex,
                    setRecyclerViewInstructions
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
                            $$"""
                                    invoke-static { }, $$EXTENSION_CLASS_DESCRIPTOR->isCommentsScrollTopEnabled()Z
                                    move-result v0
                                    if-eqz v0, :ignore

                                    # Get engagement panel id.
                                    invoke-static { p1 }, $$engagementPanelIdMethodCall
                                    move-result-object v0

                                    # Check if engagement panel id is not null.
                                    if-eqz v0, :ignore

                                    const-string v1, "comment"
                                    invoke-virtual { v0, v1 }, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                                    move-result v0

                                    # Check if engagement panel id is a comment.
                                    if-eqz v0, :ignore

                                    invoke-virtual { p0 }, $$recyclerViewOptionalMethodCall
                                    move-result-object v0
                                    invoke-virtual { v0 }, Lj$/util/Optional;->isPresent()Z
                                    move-result v1

                                    # Check if recycler view is not null.
                                    if-eqz v1, :ignore
                                    invoke-virtual { v0 }, Lj$/util/Optional;->get()Ljava/lang/Object;
                                    move-result-object v0
                                    check-cast v0, Landroid/support/v7/widget/RecyclerView;
                                    invoke-static { v0 }, $$EXTENSION_CLASS_DESCRIPTOR->onCommentsCreate(Landroid/support/v7/widget/RecyclerView;)V

                                    :ignore
                                    return-void
                                    """,
                        )
                    }
                )
            }
        }
        mapOf(
            informationButton to "hideInformationButton",
            modernTitle to "setContentHeader",
            title to "setContentHeader"
        ).forEach { (literal, methodName) ->
            engagementPanelTitleFingerprint
                .match(engagementPanelTitleParentFingerprint.classDef)
                .method
                .injectLiteralInstructionViewCall(
                    literal,
                    "invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $EXTENSION_CLASS_DESCRIPTOR->$methodName(Landroid/view/View;)V"
                )
        }

        findMethodOrThrow(EXTENSION_CLASS_DESCRIPTOR) {
            name == "smoothScrollToPosition"
        }.addInstruction(
            0,
            "invoke-virtual {p0, p1}, ${recyclerViewSmoothScrollToPositionFingerprint.method.methodCall()}"
        )
    }
}
