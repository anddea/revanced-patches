package app.morphe.patches.youtube.layout.widesearchbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.BytecodePatchContext
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
