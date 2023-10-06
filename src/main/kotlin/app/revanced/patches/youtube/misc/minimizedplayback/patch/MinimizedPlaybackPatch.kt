package app.revanced.patches.youtube.misc.minimizedplayback.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.misc.minimizedplayback.fingerprints.KidsMinimizedPlaybackPolicyControllerFingerprint
import app.revanced.patches.youtube.misc.minimizedplayback.fingerprints.MinimizedPlaybackAudioFingerprint
import app.revanced.patches.youtube.misc.minimizedplayback.fingerprints.MinimizedPlaybackFingerprint
import app.revanced.patches.youtube.misc.minimizedplayback.fingerprints.MinimizedPlaybackSettingsFingerprint
import app.revanced.patches.youtube.misc.minimizedplayback.fingerprints.PiPPlaybackFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.integrations.patch.IntegrationsPatch
import app.revanced.patches.youtube.utils.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch
@Name("Enable minimized playback")
@Description("Enables minimized and background playback.")
@DependsOn(
    [
        IntegrationsPatch::class,
        PlayerTypeHookPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class MinimizedPlaybackPatch : BytecodePatch(
    listOf(
        KidsMinimizedPlaybackPolicyControllerFingerprint,
        MinimizedPlaybackAudioFingerprint,
        MinimizedPlaybackFingerprint,
        MinimizedPlaybackSettingsFingerprint,
        PiPPlaybackFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        KidsMinimizedPlaybackPolicyControllerFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstruction(
                    0,
                    "return-void"
                )
            }
        } ?: throw KidsMinimizedPlaybackPolicyControllerFingerprint.exception

        arrayOf(
            MinimizedPlaybackAudioFingerprint,
            MinimizedPlaybackFingerprint
        ).forEach { fingerprint ->
            fingerprint.result?.let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.endIndex
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstruction(
                        insertIndex,
                        "const/4 v$insertRegister, 0x1"
                    )
                }
            } ?: throw fingerprint.exception
        }

        MinimizedPlaybackSettingsFingerprint.result?.let {
            it.mutableMethod.apply {
                val booleanCalls = implementation!!.instructions.withIndex()
                    .filter { instruction ->
                        ((instruction.value as? ReferenceInstruction)?.reference as? MethodReference)?.returnType == "Z"
                    }

                val booleanIndex = booleanCalls.elementAt(1).index
                val booleanMethod =
                    context.toMethodWalker(this)
                        .nextMethod(booleanIndex, true)
                        .getMethod() as MutableMethod

                booleanMethod.addInstructions(
                    0, """
                        const/4 v0, 0x1
                        return v0
                        """
                )
            }
        } ?: throw MinimizedPlaybackSettingsFingerprint.exception

        PiPPlaybackFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {}, $INTEGRATIONS_METHOD_REFERENCE
                        move-result v$insertRegister
                        """
                )
            }
        } ?: throw PiPPlaybackFingerprint.exception
    }

    private companion object {
        const val INTEGRATIONS_METHOD_REFERENCE =
            "$MISC_PATH/MinimizedPlaybackPatch;->isPlaybackNotShort()Z"
    }
}
