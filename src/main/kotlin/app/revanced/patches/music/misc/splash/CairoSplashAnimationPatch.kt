package app.revanced.patches.music.misc.splash

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.misc.splash.fingerprints.CairoSplashAnimationConfigFingerprint
import app.revanced.patches.music.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MainActivityLaunchAnimation
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstWideLiteralInstructionValueOrThrow
import app.revanced.util.injectLiteralInstructionBooleanCall
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Disable Cairo splash animation",
    description = "Adds an option to disable Cairo splash animation.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "7.06.54",
                "7.16.53",
            ]
        )
    ]
)
@Suppress("unused")
object CairoSplashAnimationPatch : BytecodePatch(
    setOf(CairoSplashAnimationConfigFingerprint)
) {
    private const val INTEGRATIONS_METHOD_DESCRIPTOR =
        "$MISC_PATH/CairoSplashAnimationPatch;->disableCairoSplashAnimation(Z)Z"

    override fun execute(context: BytecodeContext) {

        if (!SettingsPatch.upward0706) {
            println("WARNING: This patch is not supported in this version. Use YouTube Music 7.06.54 or later.")
            return
        } else if (!SettingsPatch.upward0720) {
            CairoSplashAnimationConfigFingerprint.injectLiteralInstructionBooleanCall(
                45635386,
                INTEGRATIONS_METHOD_DESCRIPTOR
            )
        } else {
            CairoSplashAnimationConfigFingerprint.resultOrThrow().mutableMethod.apply {
                val literalIndex = indexOfFirstWideLiteralInstructionValueOrThrow(
                    MainActivityLaunchAnimation
                )
                val insertIndex = indexOfFirstInstructionReversedOrThrow(literalIndex) {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "setContentView"
                } + 1
                val viewStubFindViewByIdIndex = indexOfFirstInstructionOrThrow(literalIndex) {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.name == "findViewById" &&
                            reference.definingClass != "Landroid/view/View;"
                }
                val freeRegister =
                    getInstruction<FiveRegisterInstruction>(viewStubFindViewByIdIndex).registerD
                val jumpIndex = indexOfFirstInstructionReversedOrThrow(
                    viewStubFindViewByIdIndex,
                    Opcode.IGET_OBJECT
                )

                addInstructionsWithLabels(
                    insertIndex, """
                        const/4 v$freeRegister, 0x1
                        invoke-static {v$freeRegister}, $INTEGRATIONS_METHOD_DESCRIPTOR
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :skip
                        """, ExternalLabel("skip", getInstruction(jumpIndex))
                )
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.MISC,
            "revanced_disable_cairo_splash_animation",
            "false"
        )

    }
}
