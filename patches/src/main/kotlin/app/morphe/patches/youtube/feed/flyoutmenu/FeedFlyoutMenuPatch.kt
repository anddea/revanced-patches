/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.feed.flyoutmenu

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.bottomSheetMenuItemBuilderFingerprint
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.FEED_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.indexOfSpannedCharSequenceInstruction
import app.morphe.patches.youtube.utils.patch.PatchList.HIDE_FEED_FLYOUT_MENU
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_FLYOUT_MENU_ITEM_INTERFACE =
    "Lapp/morphe/extension/youtube/patches/feed/FeedPatch\$FlyoutMenuItem;"

@Suppress("unused")
val feedFlyoutMenuPatch = bytecodePatch(
    HIDE_FEED_FLYOUT_MENU.title,
    HIDE_FEED_FLYOUT_MENU.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        sharedResourceIdPatch,
        settingsPatch,
    )
    execute {

        // region patch for phone

        bottomSheetMenuItemBuilderFingerprint.methodOrThrow().apply {
            val insertIndex = indexOfSpannedCharSequenceInstruction(this) + 2
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

            addInstructions(
                insertIndex, """
                    invoke-static {v$insertRegister}, $FEED_CLASS_DESCRIPTOR->hideFlyoutMenu(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                    move-result-object v$insertRegister
                    """
            )
        }

        // endregion

        // region patch for tablet

        ContextualMenuItemBuilderFingerprint.let {
            it.method.apply {
                val targetIndex = it.instructionMatches[2].index
                val targetInstruction = getInstruction<Instruction35c>(targetIndex)

                val targetReferenceName =
                    (targetInstruction.reference as MethodReference).name
                if (targetReferenceName != "setText")
                    throw PatchException("Method name did not match: $targetReferenceName")

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                            "$FEED_CLASS_DESCRIPTOR->hideFlyoutMenu(Landroid/widget/TextView;Ljava/lang/CharSequence;)V"
                )
            }
        }

        // endregion

        // The list popup menu ignores the zero height of a hidden item and only keeps its text hidden,
        // so the filtered items are left out of the items of the popup menus instead.
        // The items of the menu are filtered after they are resolved, as an item of the menu,
        // such as a conditional item, can be replaced by another item.
        val menuItemTitleMethod = ContextualMenuItemBuilderFingerprint.instructionMatches.first().methodReference

        MenuItemListBuilderFingerprint.let {
            it.method.apply {
                val menuItemType = it.instructionMatches[4].getInstruction<ReferenceInstruction>().reference.toString()
                if (menuItemTitleMethod.parameterTypes.first().toString() != menuItemType) {
                    throw PatchException("Unexpected menu item title method: $menuItemTitleMethod")
                }

                mutableClassDefBy(menuItemType).apply {
                    interfaces.add(EXTENSION_FLYOUT_MENU_ITEM_INTERFACE)
                    methods.add(
                        ImmutableMethod(
                            type,
                            "patch_getFlyoutMenuItemTitle",
                            listOf(),
                            "Ljava/lang/CharSequence;",
                            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                            null,
                            null,
                            MutableMethodImplementation(2),
                        ).toMutable().apply {
                            addInstructions(
                                0,
                                """
                                    invoke-static { p0 }, $menuItemTitleMethod
                                    move-result-object v0
                                    return-object v0
                                """
                            )
                        }
                    )
                }

                val addItemsIndex = it.instructionMatches.last().index
                val itemsRegister = getInstruction<FiveRegisterInstruction>(addItemsIndex).registerD

                addInstructions(
                    addItemsIndex,
                    """
                        invoke-static/range { v$itemsRegister .. v$itemsRegister }, $FEED_CLASS_DESCRIPTOR->hideFlyoutMenuItems(Ljava/lang/Iterable;)Ljava/lang/Iterable;
                        move-result-object v$itemsRegister
                    """
                )
            }
        }

        ActionBarMenuBuilderFingerprint.let {
            it.method.apply {
                val addIndex = it.instructionMatches.last().index
                val addInstruction = getInstruction<FiveRegisterInstruction>(addIndex)
                val insertIndex = if (getInstruction(addIndex + 1).opcode == Opcode.MOVE_RESULT_OBJECT) {
                    addIndex + 2
                } else {
                    addIndex + 1
                }

                addInstruction(
                    insertIndex,
                    "invoke-static { v${addInstruction.registerC}, v${addInstruction.registerE}, v${addInstruction.registerG} }, " +
                            "$FEED_CLASS_DESCRIPTOR->hideFlyoutMenu(Landroid/view/Menu;ILjava/lang/CharSequence;)V"
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: FEED",
                "SETTINGS: HIDE_FEED_FLYOUT_MENU"
            ),
            HIDE_FEED_FLYOUT_MENU
        )

        // endregion

    }
}
