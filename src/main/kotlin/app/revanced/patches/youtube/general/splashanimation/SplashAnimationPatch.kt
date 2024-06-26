package app.revanced.patches.youtube.general.splashanimation

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.general.splashanimation.fingerprints.SplashAnimationFingerprint
import app.revanced.patches.youtube.general.splashanimation.fingerprints.StartUpResourceIdFingerprint
import app.revanced.patches.youtube.general.splashanimation.fingerprints.StartUpResourceIdParentFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
object SplashAnimationPatch : BaseBytecodePatch(
    name = "Disable splash animation",
    description = "Adds an option to disable the splash animation on app startup.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        SplashAnimationFingerprint,
        StartUpResourceIdParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        StartUpResourceIdFingerprint.resolve(
            context,
            StartUpResourceIdParentFingerprint.resultOrThrow().classDef
        )

        val startUpResourceIdMethod = StartUpResourceIdFingerprint.resultOrThrow().mutableMethod
        val startUpResourceIdMethodCall =
            startUpResourceIdMethod.definingClass + "->" + startUpResourceIdMethod.name + "(I)Z"

        SplashAnimationFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
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

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: DISABLE_SPLASH_ANIMATION"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
