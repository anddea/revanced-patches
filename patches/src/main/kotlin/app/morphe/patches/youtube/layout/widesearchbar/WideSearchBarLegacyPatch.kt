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

package app.morphe.patches.youtube.layout.widesearchbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.youtube.general.toolbar.actionBarRingoBackgroundFingerprint
import app.morphe.patches.youtube.general.toolbar.actionBarRingoConstructorFingerprint
import app.morphe.patches.youtube.general.toolbar.actionBarRingoTextFingerprint
import app.morphe.patches.youtube.general.toolbar.indexOfActionBarRingoBackgroundTabletInstruction
import app.morphe.patches.youtube.general.toolbar.indexOfActionBarRingoTextTabletInstructions
import app.morphe.patches.youtube.general.toolbar.setActionBarRingoFingerprint
import app.morphe.patches.youtube.general.toolbar.youActionBarFingerprint
import app.morphe.patches.youtube.utils.resourceid.actionBarRingoBackground
import app.morphe.patches.youtube.utils.settings.ResourceUtils.getContext
import app.morphe.util.doRecursively
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getWalkerMethod
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val EXTENSION_CLASS = "Lapp/morphe/extension/youtube/patches/WideSearchBarLegacyPatch;"

private fun MutableMethod.injectSearchBarHook(
    insertIndex: Int,
    insertRegister: Int,
    descriptor: String
) =
    addInstructions(
        insertIndex, """
            invoke-static {v$insertRegister}, $EXTENSION_CLASS->$descriptor(Z)Z
            move-result v$insertRegister
            """
    )

private fun MutableMethod.injectSearchBarHook(
    insertIndex: Int,
    descriptor: String
) =
    injectSearchBarHook(
        insertIndex,
        getInstruction<OneRegisterInstruction>(insertIndex).registerA,
        descriptor
    )

context(_: BytecodePatchContext)
internal fun applyYouTabWideSearchBar2031() {
    YouActionBarViewFingerprint2031.apply {
        val match = instructionMatches[1]
        val index = match.index
        val register = method.getInstruction<OneRegisterInstruction>(index).registerA

        method.injectSearchBarHook(
            index,
            register,
            "enableWideSearchBarInYouTab"
        )
    }
}

/**
 * Applies the previous wide-search implementation required by YouTube versions below 20.31.
 */
context(_: BytecodePatchContext)
internal fun applyLegacyWideSearchBar() {
    // Limitation: Premium header will not be applied for YouTube Premium users if the user uses the
    // 'Wide search bar with header' option. 'Change YouTube header' is required as a workaround.
    actionBarRingoBackgroundFingerprint.methodOrThrow().apply {
        val viewIndex =
            indexOfFirstLiteralInstructionOrThrow(actionBarRingoBackground) + 2
        val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA

        addInstructions(
            viewIndex + 1,
            "invoke-static {v$viewRegister}, $EXTENSION_CLASS->setWideSearchBarLayout(Landroid/view/View;)V"
        )

        val targetIndex = indexOfActionBarRingoBackgroundTabletInstruction(this) + 1
        val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

        injectSearchBarHook(
            targetIndex + 1,
            targetRegister,
            "enableWideSearchBarWithHeaderInverse"
        )
    }

    actionBarRingoTextFingerprint.methodOrThrow(actionBarRingoBackgroundFingerprint).apply {
        val targetIndex = indexOfActionBarRingoTextTabletInstructions(this) + 1
        val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

        injectSearchBarHook(
            targetIndex + 1,
            targetRegister,
            "enableWideSearchBarWithHeader"
        )
    }

    actionBarRingoConstructorFingerprint.methodOrThrow().apply {
        val staticCalls = implementation!!.instructions
            .withIndex()
            .filter { (_, instruction) ->
                val methodReference = (instruction as? ReferenceInstruction)?.reference
                instruction.opcode == Opcode.INVOKE_STATIC &&
                        methodReference is MethodReference &&
                        methodReference.parameterTypes.size == 1 &&
                        methodReference.returnType == "Z"
            }

        if (staticCalls.size != 2)
            throw PatchException("Size of staticCalls does not match: ${staticCalls.size}")

        mapOf(
            staticCalls.elementAt(0).index to "enableWideSearchBar",
            staticCalls.elementAt(1).index to "enableWideSearchBarWithHeader"
        ).forEach { (index, descriptor) ->
            getWalkerMethod(index).apply {
                injectSearchBarHook(
                    implementation!!.instructions.lastIndex,
                    descriptor
                )
            }
        }
    }

    youActionBarFingerprint.matchOrThrow(setActionBarRingoFingerprint).let {
        it.method.apply {
            injectSearchBarHook(
                it.instructionMatches.last().index,
                "enableWideSearchBarInYouTab"
            )
        }
    }

    // This attribute cannot be changed in the extension.
    getContext().document("res/layout/action_bar_ringo_background.xml").use { document ->
        document.doRecursively { node ->
            if (node is Element) {
                node.getAttributeNode("android:layout_marginStart")?.textContent = "0.0dip"
            }
        }
    }
}
