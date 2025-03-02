package app.revanced.patches.youtube.general.splashanimation

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.patch.PatchList.DISABLE_SPLASH_ANIMATION
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
val splashAnimationPatch = bytecodePatch(
    DISABLE_SPLASH_ANIMATION.title,
    DISABLE_SPLASH_ANIMATION.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedResourceIdPatch,
        settingsPatch,
    )

    execute {

        val startUpResourceIdMethod =
            startUpResourceIdFingerprint.methodOrThrow(startUpResourceIdParentFingerprint)
        val startUpResourceIdMethodCall =
            startUpResourceIdMethod.definingClass + "->" + startUpResourceIdMethod.name + "(I)Z"

        splashAnimationFingerprint.matchOrThrow().let {
            it.method.apply {
                for (index in implementation!!.instructions.size - 1 downTo 0) {
                    val instruction = getInstruction(index)
                    if (instruction.opcode != Opcode.INVOKE_STATIC)
                        continue

                    if ((instruction as ReferenceInstruction).reference.toString() != startUpResourceIdMethodCall)
                        continue

                    val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                    addInstructions(
                        index + 2, """
                            invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->disableSplashAnimation(Z)Z
                            move-result v$register
                            """
                    )
                }
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
