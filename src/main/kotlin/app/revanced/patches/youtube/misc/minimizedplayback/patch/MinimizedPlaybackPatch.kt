package app.revanced.patches.youtube.misc.minimizedplayback.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.misc.minimizedplayback.fingerprints.KidsMinimizedPlaybackPolicyControllerFingerprint
import app.revanced.patches.youtube.misc.minimizedplayback.fingerprints.MinimizedPlaybackManagerFingerprint
import app.revanced.patches.youtube.misc.minimizedplayback.fingerprints.MinimizedPlaybackSettingsFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.integrations.patch.IntegrationsPatch
import app.revanced.patches.youtube.utils.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch
@Name("Enable minimized playback")
@Description("Enables minimized and background playback.")
@DependsOn(
    [
        PlayerTypeHookPatch::class,
        IntegrationsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class MinimizedPlaybackPatch : BytecodePatch(
    listOf(
        KidsMinimizedPlaybackPolicyControllerFingerprint,
        MinimizedPlaybackManagerFingerprint,
        MinimizedPlaybackSettingsFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val methods = arrayOf(
            KidsMinimizedPlaybackPolicyControllerFingerprint,
            MinimizedPlaybackManagerFingerprint,
            MinimizedPlaybackSettingsFingerprint
        ).map {
            it.result?.mutableMethod ?: return it.toErrorResult()
        }

        methods[0].hookKidsMiniPlayer()
        methods[1].hookMinimizedPlaybackManager()
        methods[2].hookMinimizedPlaybackSettings(context)

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_METHOD_REFERENCE =
            "$MISC_PATH/MinimizedPlaybackPatch;->isPlaybackNotShort()Z"

        fun MutableMethod.hookKidsMiniPlayer() {
            addInstruction(
                0,
                "return-void"
            )
        }

        fun MutableMethod.hookMinimizedPlaybackManager() {
            addInstructions(
                0, """
                    invoke-static {}, $INTEGRATIONS_METHOD_REFERENCE
                    move-result v0
                    return v0
                    """
            )
        }

        fun MutableMethod.hookMinimizedPlaybackSettings(
            context: BytecodeContext
        ) {
            val booleanCalls = implementation!!.instructions.withIndex()
                .filter { ((it.value as? ReferenceInstruction)?.reference as? MethodReference)?.returnType == "Z" }

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
    }
}
