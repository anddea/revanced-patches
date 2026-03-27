package app.morphe.patches.youtube.general.splashanimation

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.patch.PatchList.DISABLE_SPLASH_ANIMATION
import app.morphe.patches.youtube.utils.playservice.is_20_02_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val splashAnimationPatch = bytecodePatch(
    DISABLE_SPLASH_ANIMATION.title,
    DISABLE_SPLASH_ANIMATION.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedResourceIdPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {

        val startUpResourceIdMethod =
            startUpResourceIdFingerprint.methodOrThrow(startUpResourceIdParentFingerprint)
        val startUpResourceIdMethodCall =
            startUpResourceIdMethod.definingClass + "->" + startUpResourceIdMethod.name + "(I)Z"

        splashAnimationFingerprint.methodOrThrow().apply {
            implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    instruction.opcode == Opcode.INVOKE_STATIC &&
                            (instruction as? ReferenceInstruction)?.reference?.toString() == startUpResourceIdMethodCall
                }
                .map { (index, _) -> index }
                .reversed()
                .forEach { index ->
                    val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                    addInstructions(
                        index + 2, """
                            invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->disableSplashAnimation(Z)Z
                            move-result v$register
                            """
                    )
                }

            if (is_20_02_or_greater) {
                val animatedVectorDrawableIndex =
                    indexOfStartAnimatedVectorDrawableInstruction(this)
                val arrayIndex =
                    indexOfFirstInstructionReversedOrThrow(animatedVectorDrawableIndex) {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                reference?.returnType == "V" &&
                                reference.parameterTypes.size == 1 &&
                                reference.parameterTypes.first().startsWith("[L")
                    }

                val insertIndex =
                    indexOfFirstInstructionOrThrow(arrayIndex, Opcode.IF_NE)
                val insertInstruction = getInstruction<TwoRegisterInstruction>(insertIndex)

                addInstructions(
                    insertIndex, """
                        invoke-static {v${insertInstruction.registerA}, v${insertInstruction.registerB}}, $GENERAL_CLASS_DESCRIPTOR->disableSplashAnimation(II)I
                        move-result v${insertInstruction.registerA}
                        """
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: DISABLE_SPLASH_ANIMATION"
            ),
            DISABLE_SPLASH_ANIMATION
        )

        // endregion

    }
}
