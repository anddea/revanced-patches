package app.revanced.patches.youtube.misc.minimizedplayback

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.misc.minimizedplayback.fingerprints.KidsMinimizedPlaybackPolicyControllerFingerprint
import app.revanced.patches.youtube.misc.minimizedplayback.fingerprints.MinimizedPlaybackManagerFingerprint
import app.revanced.patches.youtube.misc.minimizedplayback.fingerprints.MinimizedPlaybackSettingsFingerprint
import app.revanced.patches.youtube.misc.minimizedplayback.fingerprints.PiPControllerFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getWalkerMethod
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
object MinimizedPlaybackPatch : BaseBytecodePatch(
    name = "Enable minimized playback",
    description = "Enables minimized and background playback.",
    dependencies = setOf(
        PlayerTypeHookPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        KidsMinimizedPlaybackPolicyControllerFingerprint,
        MinimizedPlaybackManagerFingerprint,
        MinimizedPlaybackSettingsFingerprint,
        PiPControllerFingerprint
    )
) {
    private const val INTEGRATIONS_METHOD_REFERENCE =
        "$MISC_PATH/MinimizedPlaybackPatch;->isPlaybackNotShort()Z"

    override fun execute(context: BytecodeContext) {
        KidsMinimizedPlaybackPolicyControllerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstruction(
                    0,
                    "return-void"
                )
            }
        }

        MinimizedPlaybackManagerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        invoke-static {}, $INTEGRATIONS_METHOD_REFERENCE
                        move-result v0
                        return v0
                        """
                )
            }
        }

        MinimizedPlaybackSettingsFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val booleanCalls = implementation!!.instructions.withIndex()
                    .filter { instruction ->
                        ((instruction.value as? ReferenceInstruction)?.reference as? MethodReference)?.returnType == "Z"
                    }

                val booleanIndex = booleanCalls.elementAt(1).index
                val booleanMethod = getWalkerMethod(context, booleanIndex)

                booleanMethod.addInstructions(
                    0, """
                        const/4 v0, 0x1
                        return v0
                        """
                )
            }
        }

        PiPControllerFingerprint.resultOrThrow().let {
            val targetMethod = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.endIndex)

            targetMethod.apply {
                val targetRegister = getInstruction<TwoRegisterInstruction>(0).registerA

                addInstruction(
                    1,
                    "const/4 v$targetRegister, 0x1"
                )
            }
        }

        SettingsPatch.updatePatchStatus(this)
    }
}
