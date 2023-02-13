package app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.fingerprints.*
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.util.integrations.Constants.GENERAL_LAYOUT
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("enable-tablet-miniplayer-bytecode-patch")
@DependsOn([SharedResourcdIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class TabletMiniPlayerBytecodePatch : BytecodePatch(
    listOf(
        MiniPlayerDimensionsCalculatorFingerprint,
        MiniPlayerResponseModelSizeCheckFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        MiniPlayerDimensionsCalculatorFingerprint.result?.let { parentResult ->
            // first resolve the fingerprints via the parent fingerprint
            val miniPlayerClass = parentResult.classDef

            arrayOf(
                MiniPlayerOverrideNoContextFingerprint,
                MiniPlayerOverrideFingerprint,
                MiniPlayerResponseModelSizeCheckFingerprint
            ).map {
                it to (it.also { it.resolve(context, miniPlayerClass) }.result ?: return it.toErrorResult())
            }.forEach { (fingerprint, result) ->
                if (fingerprint == MiniPlayerOverrideNoContextFingerprint) {
                    val (method, _, parameterRegister) = result.addProxyCall()
                    method.insertOverride(method.implementation!!.instructions.size - 1, parameterRegister)
                } else {
                    val (_, _, _) = result.addProxyCall()
                }
            }
        } ?: return MiniPlayerDimensionsCalculatorFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    // helper methods
    private companion object {
        fun MethodFingerprintResult.addProxyCall(): Triple<MutableMethod, Int, Int> {
            val (method, scanIndex, parameterRegister) = this.unwrap()
            method.insertOverride(scanIndex, parameterRegister)

            return Triple(method, scanIndex, parameterRegister)
        }

        fun MutableMethod.insertOverride(index: Int, overrideRegister: Int) {
            this.addInstructions(
                index,
                """
                    invoke-static {v$overrideRegister}, $GENERAL_LAYOUT->enableTabletMiniPlayer(Z)Z
                    move-result v$overrideRegister
                    """
            )
        }

        fun MethodFingerprintResult.unwrap(): Triple<MutableMethod, Int, Int> {
            val scanIndex = this.scanResult.patternScanResult!!.endIndex
            val method = this.mutableMethod
            val instructions = method.implementation!!.instructions
            val parameterRegister = (instructions[scanIndex] as OneRegisterInstruction).registerA

            return Triple(method, scanIndex, parameterRegister)
        }
    }
}
