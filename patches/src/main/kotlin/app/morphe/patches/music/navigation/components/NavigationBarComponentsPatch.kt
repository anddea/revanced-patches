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

package app.morphe.patches.music.navigation.components

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.music.general.startpage.changeStartPagePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.Constants.NAVIGATION_CLASS_DESCRIPTOR
import app.morphe.patches.music.utils.patch.PatchList.NAVIGATION_BAR_COMPONENTS
import app.morphe.patches.music.utils.playservice.is_8_51_or_greater
import app.morphe.patches.music.utils.playservice.is_9_15_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.resourceid.colorGrey
import app.morphe.patches.music.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.music.utils.resourceid.text1
import app.morphe.patches.music.utils.resourceid.ytFillSamples
import app.morphe.patches.music.utils.resourceid.ytFillYouTubeMusic
import app.morphe.patches.music.utils.resourceid.ytOutlineSamples
import app.morphe.patches.music.utils.resourceid.ytOutlineYouTubeMusic
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addPreferenceWithIntent
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.addTextPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.util.REGISTER_TEMPLATE_REPLACEMENT
import app.morphe.util.findMutableClassOrThrow
import app.morphe.util.findMutableMethodOf
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import app.morphe.util.replaceLiteralInstructionCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val FLAG = "android:layout_weight"
private const val RESOURCE_FILE_PATH = "res/layout/image_with_text_tab.xml"

private val navigationBarComponentsResourcePatch = resourcePatch(
    description = "navigationBarComponentsResourcePatch"
) {
    execute {
        document(RESOURCE_FILE_PATH).use { document ->
            with(document.getElementsByTagName("ImageView").item(0)) {
                if (attributes.getNamedItem(FLAG) != null)
                    return@with

                document.createAttribute(FLAG)
                    .apply { value = "0.5" }
                    .let(attributes::setNamedItem)
            }
        }
    }
}

@Suppress("unused")
val navigationBarComponentsPatch = bytecodePatch(
    NAVIGATION_BAR_COMPONENTS.title,
    NAVIGATION_BAR_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    dependsOn(
        changeStartPagePatch,
        navigationBarComponentsResourcePatch,
        sharedResourceIdPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        /**
         * Hook theme class constructor to dynamically override mappings
         */
        val themeMethod = ThemeMapConstructorFingerprint.method
        findMutableClassOrThrow(themeMethod.definingClass).findMutableMethodOf(themeMethod).apply {
            val returnIndex = indexOfFirstInstructionOrThrow(Opcode.RETURN_VOID)
            addInstruction(
                returnIndex,
                "invoke-static/range {p0 .. p0}, $NAVIGATION_CLASS_DESCRIPTOR->onThemeClassInit(Ljava/lang/Object;)V"
            )
        }



        /**
         * Enable custom navigation bar color
         */
        tabLayoutFingerprint.methodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(colorGrey)
            val insertIndex = indexOfFirstInstructionOrThrow(constIndex) {
                opcode == Opcode.INVOKE_VIRTUAL
                        && getReference<MethodReference>()?.name == "setBackgroundColor"
            }
            val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

            addInstructions(
                insertIndex, """
                    invoke-static {}, $NAVIGATION_CLASS_DESCRIPTOR->enableCustomNavigationBarColor()I
                    move-result v$insertRegister
                    """
            )
        }

        /**
         * Hide navigation labels
         */
        if (!is_8_51_or_greater) {
            legacyTabLayoutTextFingerprint.methodOrThrow().apply {
                val constIndex =
                    indexOfFirstLiteralInstructionOrThrow(text1)
                val targetIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)
                val targetParameter = getInstruction<ReferenceInstruction>(targetIndex).reference
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                if (!targetParameter.toString().endsWith("Landroid/widget/TextView;"))
                    throw PatchException("Method signature parameter did not match: $targetParameter")

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $NAVIGATION_CLASS_DESCRIPTOR->hideNavigationLabel(Landroid/widget/TextView;)V"
                )
            }
        } else {
            /**
             * YouTube Music 9.15 keeps the navigation entity model but moves tab
             * rendering into a synthetic callback. Hook its resolved values so
             * the existing runtime replacement behavior can be reused.
             */
            TabLayoutTextFingerprint.let { fingerprint ->
                fingerprint.method.apply {
                    // Apply in reverse order so the fingerprint match indices remain valid.
                    val pivotTabIndex = fingerprint.instructionMatches.last().index
                    val pivotTabRegister =
                        getInstruction<FiveRegisterInstruction>(pivotTabIndex).registerC
                    val enumIndex = fingerprint.instructionMatches[7].index
                    val enumRegister = getInstruction<OneRegisterInstruction>(enumIndex).registerA
                    val labelIndex = fingerprint.instructionMatches[3].index
                    val labelRegister = getInstruction<OneRegisterInstruction>(labelIndex).registerA

                    val replacementEnabled = is_9_15_or_greater
                    val spannedIndex = if (replacementEnabled) indexOfSetTextInstruction(this) else -1
                    val spannedRegister = if (replacementEnabled) {
                        getInstruction<FiveRegisterInstruction>(spannedIndex).registerD
                    } else {
                        -1
                    }
                    val iconIndex = if (replacementEnabled) {
                        indexOfFirstInstructionOrThrow {
                            opcode == Opcode.INVOKE_VIRTUAL &&
                                    getReference<MethodReference>()?.name == "setImageResource"
                        }
                    } else {
                        -1
                    }
                    val iconRegister = if (replacementEnabled) {
                        getInstruction<FiveRegisterInstruction>(iconIndex).registerD
                    } else {
                        -1
                    }
                    val navigationItemIndex = if (replacementEnabled) {
                        indexOfFirstInstructionReversedOrThrow(labelIndex) {
                            opcode == Opcode.CHECK_CAST &&
                                    getReference<TypeReference>()?.type == "Lbxjp;"
                        }
                    } else {
                        -1
                    }
                    val navigationItemRegister = if (replacementEnabled) {
                        getInstruction<OneRegisterInstruction>(navigationItemIndex).registerA
                    } else {
                        -1
                    }
                    val browseIdIndex = if (replacementEnabled) {
                        indexOfFirstInstructionOrThrow(navigationItemIndex) {
                            opcode == Opcode.IGET_OBJECT &&
                                    getReference<FieldReference>()?.let { field ->
                                        field.definingClass == "Lbxjp;" &&
                                                field.type == "Ljava/lang/String;"
                                    } == true
                        }
                    } else {
                        -1
                    }
                    val browseIdInstruction = if (replacementEnabled) {
                        getInstruction<TwoRegisterInstruction>(browseIdIndex)
                    } else {
                        null
                    }
                    val browseIdRegister = browseIdInstruction?.registerA ?: -1
                    val browseIdField = if (replacementEnabled) {
                        getInstruction<ReferenceInstruction>(browseIdIndex).reference as FieldReference
                    } else {
                        null
                    }
                    val fieldNameRegister = if (replacementEnabled) {
                        implementation!!.registerCount - parameters.size - 2
                    } else {
                        -1
                    }

                    addInstruction(
                        pivotTabIndex,
                        "invoke-static {v$pivotTabRegister}, $NAVIGATION_CLASS_DESCRIPTOR->hideNavigationButton(Landroid/view/View;)V"
                    )

                    if (replacementEnabled) {
                        addInstructions(
                            spannedIndex, """
                                invoke-static {v$spannedRegister}, $NAVIGATION_CLASS_DESCRIPTOR->replaceNavigationLabel(Landroid/text/Spanned;)Landroid/text/Spanned;
                                move-result-object v$spannedRegister
                                """
                        )
                        addInstruction(
                            iconIndex, """
                                invoke-static {v$iconRegister}, $NAVIGATION_CLASS_DESCRIPTOR->replaceNavigationIcon(I)I
                                move-result v$iconRegister
                                """
                        )
                    }

                    addInstruction(
                        enumIndex + 1,
                        "invoke-static {v$enumRegister}, $NAVIGATION_CLASS_DESCRIPTOR->setLastAppNavigationEnum(Ljava/lang/Enum;)V"
                    )
                    addInstruction(
                        labelIndex + 1,
                        "invoke-static {v$labelRegister}, $NAVIGATION_CLASS_DESCRIPTOR->hideNavigationLabel(Landroid/widget/TextView;)V"
                    )

                    if (replacementEnabled) {
                        val field = browseIdField!!
                        addInstructions(
                            navigationItemIndex + 1, """
                                iget-object v$browseIdRegister, v$navigationItemRegister, $field
                                const-string v$fieldNameRegister, "${field.name}"
                                invoke-static {v$navigationItemRegister, v$browseIdRegister, v$fieldNameRegister}, $NAVIGATION_CLASS_DESCRIPTOR->replaceBrowseId(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                                move-result-object v$browseIdRegister
                                """
                        )
                    }
                }
            }
        }

        /**
         * Hide navigation bar & buttons
         */
        if (!is_8_51_or_greater) {
            legacyTabLayoutTextFingerprint.matchOrThrow().let {
                it.method.apply {
                    val mapIndex = indexOfMapInstruction(this)
                    val browseIdRegister =
                        getInstruction<FiveRegisterInstruction>(mapIndex).registerD
                    val browseIdIndex = indexOfFirstInstructionReversedOrThrow(mapIndex + 1) {
                        opcode == Opcode.IGET_OBJECT &&
                                getReference<FieldReference>()?.type == "Ljava/lang/String;" &&
                                (this as TwoRegisterInstruction).registerA == browseIdRegister
                    }
                    val browseIdClassRegister =
                        getInstruction<TwoRegisterInstruction>(browseIdIndex).registerB
                    val browseIdFieldName =
                        (getInstruction<ReferenceInstruction>(browseIdIndex).reference as FieldReference).name

                    val enumIndex = it.instructionMatches.first().index + 3
                    val enumRegister = getInstruction<OneRegisterInstruction>(enumIndex).registerA
                    val insertEnumIndex = indexOfFirstInstructionOrThrow(Opcode.AND_INT_LIT8) - 2

                    val pivotTabIndex = indexOfGetVisibilityInstruction(this)
                    val pivotTabRegister =
                        getInstruction<FiveRegisterInstruction>(pivotTabIndex).registerC

                    val spannedIndex = indexOfSetTextInstruction(this)
                    val spannedRegister =
                        getInstruction<FiveRegisterInstruction>(spannedIndex).registerD

                    addInstruction(
                        pivotTabIndex,
                        "invoke-static {v$pivotTabRegister}, $NAVIGATION_CLASS_DESCRIPTOR->hideNavigationButton(Landroid/view/View;)V"
                    )

                    addInstructions(
                        mapIndex, """
                            const-string v$enumRegister, "$browseIdFieldName"
                            invoke-static {v$browseIdClassRegister, v$browseIdRegister, v$enumRegister}, $NAVIGATION_CLASS_DESCRIPTOR->replaceBrowseId(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v$browseIdRegister
                            """
                    )

                    addInstructions(
                        spannedIndex, """
                            invoke-static {v$spannedRegister}, $NAVIGATION_CLASS_DESCRIPTOR->replaceNavigationLabel(Landroid/text/Spanned;)Landroid/text/Spanned;
                            move-result-object v$spannedRegister
                            """
                    )

                    addInstruction(
                        insertEnumIndex,
                        "invoke-static {v$enumRegister}, $NAVIGATION_CLASS_DESCRIPTOR->setLastAppNavigationEnum(Ljava/lang/Enum;)V"
                    )
                }
            }

            val smaliInstruction = """
                invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $NAVIGATION_CLASS_DESCRIPTOR->replaceNavigationIcon(I)I
                move-result v$REGISTER_TEMPLATE_REPLACEMENT
                """

            arrayOf(
                ytFillSamples,
                ytFillYouTubeMusic,
                ytOutlineSamples,
                ytOutlineYouTubeMusic,
            ).forEach { literal ->
                replaceLiteralInstructionCall(literal, smaliInstruction)
            }
        }

        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_enable_custom_navigation_bar_color",
            "false"
        )
        addTextPreference(
            CategoryType.NAVIGATION,
            "revanced_custom_navigation_bar_color_value",
            "revanced_enable_custom_navigation_bar_color"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_home_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_samples_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_explore_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_library_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_upgrade_button",
            "true"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_replace_navigation_samples_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_replace_navigation_upgrade_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_bar",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_label",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.NAVIGATION,
            "revanced_replace_navigation_button_about"
        )

        updatePatchStatus(NAVIGATION_BAR_COMPONENTS)

    }
}
