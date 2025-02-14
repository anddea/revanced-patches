package app.revanced.patches.music.utils.navigation

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.extension.Constants.SHARED_PATH
import app.revanced.patches.music.utils.extension.sharedExtensionPatch
import app.revanced.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "$SHARED_PATH/NavigationBar;"

val navigationBarHookPatch = bytecodePatch(
    description = "navigationBarHookPatch",
) {
    dependsOn(sharedExtensionPatch)

    execute {
        tabLayoutViewSetSelectedFingerprint.methodOrThrow().apply {
            val childAtIndex = indexOfChildAtInstruction(this)
            val tabIndexRegister =
                getInstruction<FiveRegisterInstruction>(childAtIndex).registerD

            implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    val reference = (instruction as? ReferenceInstruction)?.reference
                    reference is MethodReference &&
                            reference.name == "setActivated"
                }
                .map { (index, _) -> index }
                .reversed()
                .forEach { index ->
                    val isSelectedRegister =
                        getInstruction<FiveRegisterInstruction>(childAtIndex).registerD

                    addInstruction(
                        index,
                        "invoke-static {v$tabIndexRegister, v$isSelectedRegister}, " +
                                "$EXTENSION_CLASS_DESCRIPTOR->navigationTabSelected(IZ)V"
                    )
                }
        }
    }
}
