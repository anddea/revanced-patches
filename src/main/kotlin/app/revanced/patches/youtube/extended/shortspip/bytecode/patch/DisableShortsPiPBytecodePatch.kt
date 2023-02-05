package app.revanced.patches.youtube.extended.shortspip.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.youtube.extended.shortspip.bytecode.fingerprints.*
import app.revanced.patches.youtube.misc.playertype.patch.PlayerTypeHookPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.toErrorResult
import app.revanced.shared.util.integrations.Constants.EXTENDED_PATH
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("disable-shorts-pip-bytecode-patch")
@DependsOn(
    [
        PlayerTypeHookPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class DisableShortsPiPBytecodePatch : BytecodePatch(
    listOf(
        PiPParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        PiPParentFingerprint.result?.let { result ->
            val targetIndex = result.scanResult.patternScanResult!!.endIndex
            val targetTnstuction = result.mutableMethod.instruction(targetIndex)
            val imageButtonClass =
                context.findClass((targetTnstuction as BuilderInstruction21c)
                    .reference.toString())!!
                    .mutableClass

            PiPFingerprint.also { it.resolve(context, imageButtonClass) }.result?.let { newResult ->
                with (newResult.mutableMethod) {
                    val endIndex = newResult.scanResult.patternScanResult!!.endIndex
                    val register = (implementation!!.instructions[endIndex] as OneRegisterInstruction).registerA
                    this.addInstructions(
                        endIndex + 1, """
                            invoke-static {v$register}, $EXTENDED_PATH/DisableShortsPiPPatch;->disableShortsPlayerPiP(Z)Z
                            move-result v$register
                        """
                    )
                }
            } ?: return PiPFingerprint.toErrorResult()
        } ?: return PiPParentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}