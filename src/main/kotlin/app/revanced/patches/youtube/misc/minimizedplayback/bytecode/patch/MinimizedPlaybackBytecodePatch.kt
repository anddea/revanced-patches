package app.revanced.patches.youtube.misc.minimizedplayback.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.misc.minimizedplayback.bytecode.fingerprints.*
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.MISC_PATH
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.MethodReference

@Name("enable-minimized-playback-bytecode-patch")
@DependsOn([SharedResourcdIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class MinimizedPlaybackBytecodePatch : BytecodePatch(
    listOf(
        MinimizedPlaybackKidsFingerprint,
        MinimizedPlaybackManagerFingerprint,
        MinimizedPlaybackSettingsFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val methods = arrayOf(
            MinimizedPlaybackKidsFingerprint,
            MinimizedPlaybackManagerFingerprint,
            MinimizedPlaybackSettingsFingerprint
        ).map {
            it.result!!.mutableMethod
        }
        
        hookPlaybackController(methods[0])
        hookPlayback(methods[1])
        walkMutable(context, methods[2])

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_PLAYBACK_METHOD_REFERENCE =
            "$MISC_PATH/MinimizedPlaybackPatch;->enableMinimizedPlayback()Z"

        fun walkMutable(
            context: BytecodeContext,
            insertMethod: MutableMethod
        ) {
            val booleanCalls = insertMethod.implementation!!.instructions.withIndex()
                .filter { ((it.value as? ReferenceInstruction)?.reference as? MethodReference)?.returnType == "Z" }

            val booleanIndex = booleanCalls.elementAt(1).index
            val booleanMethod =
                context.toMethodWalker(insertMethod)
                .nextMethod(booleanIndex, true)
                .getMethod() as MutableMethod

            hookPlayback(booleanMethod)
        }

        fun hookPlayback(
            insertMethod: MutableMethod
        ) {
            insertMethod.addInstructions(
                0, """
                    invoke-static {}, $INTEGRATIONS_PLAYBACK_METHOD_REFERENCE
                    move-result v0
                    return v0
                """
            )
        }

        fun hookPlaybackController(
            insertMethod: MutableMethod
        ) {
            insertMethod.addInstructions(
                0, """
                    invoke-static {}, $INTEGRATIONS_PLAYBACK_METHOD_REFERENCE
                    move-result v0
                    if-eqz v0, :default
                    return-void
                """, listOf(ExternalLabel("default", insertMethod.instruction(0)))
            )
        }
    }
}
