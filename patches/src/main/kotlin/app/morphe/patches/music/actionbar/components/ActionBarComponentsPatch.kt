/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - Hoàng Gia Bảo (https://github.com/YT-Advanced)
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

package app.morphe.patches.music.actionbar.components

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.music.utils.ACTION_BAR_POSITION_FEATURE_FLAG
import app.morphe.patches.music.utils.actionBarPositionFeatureFlagFingerprint
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.Constants.ACTIONBAR_CLASS_DESCRIPTOR
import app.morphe.patches.music.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.music.utils.patch.PatchList.HIDE_ACTION_BAR_COMPONENTS
import app.morphe.patches.music.utils.playservice.is_7_17_or_greater
import app.morphe.patches.music.utils.playservice.is_7_25_or_greater
import app.morphe.patches.music.utils.playservice.is_7_33_or_greater
import app.morphe.patches.music.utils.playservice.is_9_00_or_greater
import app.morphe.patches.music.utils.playservice.is_9_15_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.resourceid.elementsLottieAnimationViewTagId
import app.morphe.patches.music.utils.resourceid.likeDislikeContainer
import app.morphe.patches.music.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addPreferenceWithIntent
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.music.video.information.videoInformationPatch
import app.morphe.patches.shared.conversionContextFingerprintToString2
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.textcomponent.hookSpannableString
import app.morphe.patches.shared.textcomponent.textComponentPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findFieldFromToString
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import kotlin.math.min

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ActionButtonsFilter;"
private const val EXTENSION_BUTTON_PROTO_INTERFACE =
    $$"Lapp/morphe/extension/music/patches/components/ActionButtonsFilter$ButtonProtoBufferInterface;"
private const val EXTENSION_LITHO_CONTAINER_INTERFACE =
    $$"Lapp/morphe/extension/music/patches/components/ActionButtonsFilter$LithoGetBufferContainerInterface;"

@Suppress("unused")
val actionBarComponentsPatch = bytecodePatch(
    HIDE_ACTION_BAR_COMPONENTS.title,
    HIDE_ACTION_BAR_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        sharedResourceIdPatch,
        textComponentPatch,
        videoInformationPatch,
        versionCheckPatch,
    )

    execute {
        if (is_7_17_or_greater) {
            addLithoFilter(FILTER_CLASS_DESCRIPTOR)
            hookSpannableString(ACTIONBAR_CLASS_DESCRIPTOR, "onLithoTextLoaded")

            commandResolverFingerprint.methodOrThrow().addInstruction(
                0,
                "invoke-static {p2}, $ACTIONBAR_CLASS_DESCRIPTOR->inAppDownloadButtonOnClick(Ljava/util/Map;)Z"
            )

            offlineVideoEndpointFingerprint.methodOrThrow().addInstructionsWithLabels(
                0, """
                    invoke-static {p2}, $ACTIONBAR_CLASS_DESCRIPTOR->inAppDownloadButtonOnClick(Ljava/util/Map;)Z
                    move-result v0
                    if-eqz v0, :ignore
                    return-void
                    :ignore
                    nop
                    """
            )

            var lazyCallbackClassType: String? = null
            var lazyCallbackElementType: String? = null

            ComponentListFingerprint.method.apply {
                val conversionContextMatch = conversionContextFingerprintToString2.matchOrThrow()
                val conversionContextMethod = conversionContextMatch.method
                val identifierReference =
                    conversionContextMethod.findFieldFromToString("identifierProperty=")
                val identifierGetterName = conversionContextMatch.originalClassDef.methods
                    .first { method ->
                        method.name != "toString" &&
                                method.parameters.isEmpty() &&
                                method.returnType == "Ljava/lang/String;" &&
                                method.implementation?.instructions?.any {
                                    it.opcode == Opcode.IGET_OBJECT &&
                                            it.getReference<FieldReference>()?.toString() ==
                                            identifierReference.toString()
                                } == true
                    }.name
                val conversionContextType = parameters[1].type
                val elementType = parameters[2].type

                val listIndex = implementation!!.instructions.lastIndex
                val listRegister = getInstruction<OneRegisterInstruction>(listIndex).registerA
                val identifierRegister = getFreeRegisterProvider(listIndex, 1, listRegister)
                    .getFreeRegister()

                addInstructionsAtControlFlowLabel(
                    listIndex, """
                        move-object/from16 v$identifierRegister, p2
                        invoke-virtual {v$identifierRegister}, $conversionContextType->$identifierGetterName()Ljava/lang/String;
                        move-result-object v$identifierRegister
                        invoke-static {v$listRegister, v$identifierRegister}, $FILTER_CLASS_DESCRIPTOR->onLazilyConvertedElementLoaded(Ljava/util/List;Ljava/lang/String;)V
                        """
                )

                val childElementIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_INTERFACE &&
                            getReference<MethodReference>()?.let { reference ->
                                reference.definingClass == elementType &&
                                        reference.returnType == elementType &&
                                        reference.parameterTypes.size == 1 &&
                                        reference.parameterTypes.first().toString() == "I"
                            } == true
                }
                val lazyCallbackIndex = indexOfFirstInstructionOrThrow(childElementIndex) {
                    opcode == Opcode.NEW_INSTANCE &&
                            getReference<TypeReference>()?.type?.let { type ->
                                try {
                                    classDefBy(type).fields.any { field ->
                                        field.type == elementType
                                    }
                                } catch (_: Exception) {
                                    false
                                }
                            } == true
                }
                lazyCallbackClassType =
                    (getInstruction<ReferenceInstruction>(lazyCallbackIndex).reference as TypeReference).type
                lazyCallbackElementType = elementType
            }

            ButtonProtoBufferGetterFingerprint.let {
                val getterMethod = it.method
                it.classDef.apply {
                    interfaces.add(EXTENSION_BUTTON_PROTO_INTERFACE)
                    methods.add(
                        ImmutableMethod(
                            type,
                            "patch_getBuffer",
                            listOf(),
                            "[B",
                            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                            null,
                            null,
                            MutableMethodImplementation(2),
                        ).toMutable().apply {
                            addInstructions(
                                0,
                                """
                                    invoke-virtual { p0 }, $type->${getterMethod.name}()[B
                                    move-result-object v0
                                    return-object v0
                                    """
                            )
                        }
                    )
                }
            }

            fun addLithoContainerInterface(
                clazz: MutableClass,
                reference: FieldReference
            ) {
                clazz.apply {
                    if (!interfaces.contains(EXTENSION_LITHO_CONTAINER_INTERFACE)) {
                        interfaces.add(EXTENSION_LITHO_CONTAINER_INTERFACE)
                    }
                    if (methods.none { it.name == "patch_getContainer" }) {
                        methods.add(
                            ImmutableMethod(
                                type,
                                "patch_getContainer",
                                listOf(),
                                "Ljava/lang/Object;",
                                AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                                null,
                                null,
                                MutableMethodImplementation(2),
                            ).toMutable().apply {
                                addInstructions(
                                    0,
                                    """
                                        iget-object v0, p0, $reference
                                        return-object v0
                                        """
                                )
                            }
                        )
                    }
                }
            }

            TreeNodeListFingerprint.let {
                val field = it.method
                    .getInstruction<ReferenceInstruction>(it.instructionMatches.first().index)
                    .reference as FieldReference
                addLithoContainerInterface(it.classDef, field)
            }

            TreeNodeListHelperConstructorFingerprint.let {
                val p2Register = it.method.implementation!!.registerCount - it.method.parameters.size + 1
                val index = it.method.indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IPUT_OBJECT && (this as TwoRegisterInstruction).registerA == p2Register
                }
                val field = it.method.getInstruction<ReferenceInstruction>(index)
                    .reference as FieldReference

                addLithoContainerInterface(it.classDef, field)
            }

            // v8.30 lazy list entries wrap the real button element in the synthetic conversion
            // callback. Expose that captured element so the list hook can remove the whole cell.
            lazyCallbackClassType?.let { callbackClassType ->
                val callbackClass = mutableClassDefBy(callbackClassType)
                val elementField = callbackClass.fields.single {
                    it.type == checkNotNull(lazyCallbackElementType)
                }
                addLithoContainerInterface(callbackClass, elementField)
            }

            if (is_7_25_or_greater) {
                actionBarPositionFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                    ACTION_BAR_POSITION_FEATURE_FLAG,
                    "$ACTIONBAR_CLASS_DESCRIPTOR->changeActionBarPosition(Z)Z"
                )

                addSwitchPreference(
                    CategoryType.ACTION_BAR,
                    "revanced_change_action_bar_position",
                    "false"
                )
            }
        }

        if (!is_7_25_or_greater) {
            actionBarComponentFingerprint.matchOrThrow().let {
                it.method.apply {
                    // hook download button
                    val addViewIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                getReference<MethodReference>()?.name == "addView"
                    }
                    val addViewRegister =
                        getInstruction<FiveRegisterInstruction>(addViewIndex).registerD

                    addInstruction(
                        addViewIndex + 1,
                        "invoke-static {v$addViewRegister}, $ACTIONBAR_CLASS_DESCRIPTOR->inAppDownloadButtonOnClick(Landroid/view/View;)V"
                    )

                    // hide action button label
                    val noLabelIndex = indexOfFirstInstructionOrThrow {
                        val reference = (this as? ReferenceInstruction)?.reference.toString()
                        opcode == Opcode.INVOKE_DIRECT &&
                                reference.endsWith("<init>(Landroid/content/Context;)V") &&
                                !reference.contains("Lcom/google/android/libraries/youtube/common/ui/YouTubeButton;")
                    } - 2
                    val replaceIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.INVOKE_DIRECT &&
                                (this as? ReferenceInstruction)?.reference.toString()
                                    .endsWith("Lcom/google/android/libraries/youtube/common/ui/YouTubeButton;-><init>(Landroid/content/Context;)V")
                    } - 2
                    val replaceInstruction = getInstruction<TwoRegisterInstruction>(replaceIndex)
                    val replaceReference =
                        getInstruction<ReferenceInstruction>(replaceIndex).reference

                    addInstructionsWithLabels(
                        replaceIndex + 1, """
                            invoke-static {}, $ACTIONBAR_CLASS_DESCRIPTOR->hideActionBarLabel()Z
                            move-result v${replaceInstruction.registerA}
                            if-nez v${replaceInstruction.registerA}, :hidden
                            iget-object v${replaceInstruction.registerA}, v${replaceInstruction.registerB}, $replaceReference
                            """, ExternalLabel("hidden", getInstruction(noLabelIndex))
                    )
                    removeInstruction(replaceIndex)

                    // hide action button
                    val hasNextIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.INVOKE_INTERFACE &&
                                getReference<MethodReference>()?.name == "hasNext"
                    }
                    val freeRegister = min(implementation!!.registerCount - parameters.size - 2, 15)

                    val spannedIndex = indexOfFirstInstructionOrThrow {
                        getReference<MethodReference>()?.returnType == "Landroid/text/Spanned;"
                    }
                    val spannedRegister =
                        getInstruction<FiveRegisterInstruction>(spannedIndex).registerC
                    val spannedReference =
                        getInstruction<ReferenceInstruction>(spannedIndex).reference

                    addInstructionsWithLabels(
                        spannedIndex + 1, """
                            invoke-static {}, $ACTIONBAR_CLASS_DESCRIPTOR->hideActionButton()Z
                            move-result v$freeRegister
                            if-nez v$freeRegister, :hidden
                            invoke-static {v$spannedRegister}, $spannedReference
                            """, ExternalLabel("hidden", getInstruction(hasNextIndex))
                    )
                    removeInstruction(spannedIndex)

                    // set action button identifier
                    val buttonTypeDownloadIndex = it.instructionMatches.first().index + 1
                    val buttonTypeDownloadRegister =
                        getInstruction<OneRegisterInstruction>(buttonTypeDownloadIndex).registerA

                    val buttonTypeIndex = it.instructionMatches.last().index - 1
                    val buttonTypeRegister =
                        getInstruction<OneRegisterInstruction>(buttonTypeIndex).registerA

                    addInstruction(
                        buttonTypeIndex + 2,
                        "invoke-static {v$buttonTypeRegister}, $ACTIONBAR_CLASS_DESCRIPTOR->setButtonType(Ljava/lang/Object;)V"
                    )

                    addInstruction(
                        buttonTypeDownloadIndex,
                        "invoke-static {v$buttonTypeDownloadRegister}, $ACTIONBAR_CLASS_DESCRIPTOR->setButtonTypeDownload(I)V"
                    )
                }
            }
        }

        if (likeDislikeContainer != -1L) {
            likeDislikeContainerFingerprint.methodOrThrow().apply {
                val literalIndex = indexOfFirstLiteralInstructionOrThrow(likeDislikeContainer)
                val findViewByIdIndex = indexOfFirstInstructionOrThrow(literalIndex) {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.name == "findViewById" &&
                            reference.returnType == "Landroid/view/View;"
                }
                val moveResultIndex =
                    indexOfFirstInstructionOrThrow(findViewByIdIndex, Opcode.MOVE_RESULT_OBJECT)
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(moveResultIndex).registerA

                addInstruction(
                    moveResultIndex + 1,
                    "invoke-static {v$insertRegister}, $ACTIONBAR_CLASS_DESCRIPTOR->hideLikeDislikeButton(Landroid/view/View;)V"
                )
            }
        }

        val (abstractClass, lottieAnimationUrlMethodName) =
            with (lottieAnimationViewTagFingerprint.methodOrThrow()) {
                val literalIndex =
                    indexOfFirstLiteralInstructionOrThrow(elementsLottieAnimationViewTagId)
                val lottieAnimationUrlIndex =
                    indexOfFirstInstructionReversedOrThrow(literalIndex) {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_INTERFACE &&
                                reference?.returnType == "Ljava/lang/String;" &&
                                reference.parameterTypes.isEmpty()
                    }

                val lottieAnimationUrlMethodReference =
                    getInstruction<ReferenceInstruction>(lottieAnimationUrlIndex).reference as MethodReference

                Pair(
                    lottieAnimationUrlMethodReference.definingClass,
                    lottieAnimationUrlMethodReference.name,
                )
            }

        val lottieAnimationUrlFingerprint = "lottieAnimationUrlFingerprint" to Fingerprint(
            returnType = "Ljava/lang/String;",
            accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
            parameters = emptyList(),
            custom = { method, classDef ->
                classDef.interfaces.contains(abstractClass) &&
                        method.name == lottieAnimationUrlMethodName &&
                        classDef.fields.find { it.type.endsWith("Lcom/google/android/libraries/elements/adl/UpbMiniTable;") } == null
            }
        )

        lottieAnimationUrlFingerprint.methodOrThrow().apply {
            val index = implementation!!.instructions.lastIndex
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstructions(
                index, """
                    invoke-static { v$register }, $ACTIONBAR_CLASS_DESCRIPTOR->replaceLikeButton(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$register
                    """
            )
        }

        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_button_like_dislike",
            "false"
        )
        if (is_9_15_or_greater) {
            addSwitchPreference(
                CategoryType.ACTION_BAR,
                "revanced_hide_action_button_lyrics",
                "false"
            )
        }
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_button_comment",
            "false"
        )
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_button_add_to_playlist",
            "false"
        )
        if (is_9_00_or_greater) {
            addSwitchPreference(
                CategoryType.ACTION_BAR,
                "revanced_hide_action_button_details",
                "false"
            )
        }
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_button_share",
            "false"
        )
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_button_download",
            "false"
        )
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "revanced_hide_action_button_radio",
            "false"
        )
        if (is_9_00_or_greater) {
            addSwitchPreference(
                CategoryType.ACTION_BAR,
                "revanced_hide_action_button_live_chat_replay",
                "false"
            )
        }
        if (is_7_33_or_greater) {
            addSwitchPreference(
                CategoryType.ACTION_BAR,
                "revanced_hide_action_button_song_video",
                "false"
            )
        }
        if (is_7_25_or_greater) {
            addSwitchPreference(
                CategoryType.ACTION_BAR,
                "revanced_hide_action_button_disabled",
                "false"
            )
        } else {
            addSwitchPreference(
                CategoryType.ACTION_BAR,
                "revanced_hide_action_button_label",
                "false"
            )
        }
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "revanced_external_downloader_action",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.ACTION_BAR,
            "revanced_external_downloader_package_name",
            "revanced_external_downloader_action"
        )
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "revanced_replace_action_button_like",
            "false"
        )
        addSwitchPreference(
            CategoryType.ACTION_BAR,
            "revanced_replace_action_button_like_type",
            "false",
            "revanced_replace_action_button_like"
        )

        updatePatchStatus(HIDE_ACTION_BAR_COMPONENTS)

    }
}
