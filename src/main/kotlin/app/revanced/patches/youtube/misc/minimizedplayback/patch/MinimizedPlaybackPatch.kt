package app.revanced.patches.youtube.misc.minimizedplayback.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.minimizedplayback.fingerprints.*
import app.revanced.patches.youtube.misc.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.reference.MethodReference

@Patch
@Name("enable-minimized-playback")
@Description("Enables minimized and background playback.")
@DependsOn(
    [
        PlayerTypeHookPatch::class,
        SettingsPatch::class,
        SharedResourcdIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class MinimizedPlaybackPatch : BytecodePatch(
    listOf(
        KidsMinimizedPlaybackPolicyControllerFingerprint,
        MinimizedPlaybackManagerFingerprint,
        MinimizedPlaybackSettingsFingerprint,
        PipControllerFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val methods = arrayOf(
            KidsMinimizedPlaybackPolicyControllerFingerprint,
            MinimizedPlaybackManagerFingerprint,
            MinimizedPlaybackSettingsFingerprint
        ).map {
            it.result?.mutableMethod?: return it.toErrorResult()
        }

        methods[0].hookPlaybackController()
        methods[1].hookPlayback()
        methods[2].walkMutable(context)

        PipControllerFingerprint.result?.hookShortsPiP()?:return PipControllerFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_MINIMIZED_PLAYBACK"
            )
        )

        SettingsPatch.updatePatchStatus("enable-minimized-playback")

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_PLAYBACK_METHOD_REFERENCE =
            "$MISC_PATH/MinimizedPlaybackPatch;->enableMinimizedPlayback()Z"

        const val INTEGRATIONS_PIP_METHOD_REFERENCE =
            "$MISC_PATH/MinimizedPlaybackPatch;->isNotPlayingShorts(Z)Z"

        fun MutableMethod.walkMutable(
            context: BytecodeContext
        ) {
            val booleanCalls = implementation!!.instructions.withIndex()
                .filter { ((it.value as? ReferenceInstruction)?.reference as? MethodReference)?.returnType == "Z" }

            val booleanIndex = booleanCalls.elementAt(1).index
            val booleanMethod =
                context.toMethodWalker(this)
                .nextMethod(booleanIndex, true)
                .getMethod() as MutableMethod

            booleanMethod.hookPlayback()
        }

        fun MutableMethod.hookPlayback() {
            addInstructions(
                0, """
                    invoke-static {}, $INTEGRATIONS_PLAYBACK_METHOD_REFERENCE
                    move-result v0
                    return v0
                """
            )
        }

        fun MutableMethod.hookPlaybackController() {
            addInstructions(
                0, """
                    invoke-static {}, $INTEGRATIONS_PLAYBACK_METHOD_REFERENCE
                    move-result v0
                    if-eqz v0, :default
                    return-void
                """, listOf(ExternalLabel("default", instruction(0)))
            )
        }

        fun MethodFingerprintResult.hookShortsPiP() {
            val endIndex = scanResult.patternScanResult!!.endIndex
            with (mutableMethod) {
                val register = (implementation!!.instructions[endIndex] as TwoRegisterInstruction).registerA
                addInstructions(
                    endIndex + 1, """
                        invoke-static {v$register}, $INTEGRATIONS_PIP_METHOD_REFERENCE
                        move-result v$register
                    """
                )
            }
        }
    }
}
