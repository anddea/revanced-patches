package app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.fingerprints.MiniPlayerDimensionsCalculatorFingerprint
import app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.fingerprints.MiniPlayerOverrideFingerprint
import app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.fingerprints.MiniPlayerOverrideNoContextFingerprint
import app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.fingerprints.MiniPlayerResponseModelSizeCheckFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("enable-tablet-miniplayer-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class TabletMiniPlayerBytecodePatch : BytecodePatch(
    listOf(
        MiniPlayerDimensionsCalculatorFingerprint,
        MiniPlayerResponseModelSizeCheckFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        // first resolve the fingerprints via the parent fingerprint
        val miniPlayerClass = MiniPlayerDimensionsCalculatorFingerprint.result!!.classDef

        /*
         * no context parameter method
         */
        MiniPlayerOverrideNoContextFingerprint.resolve(context, miniPlayerClass)
        val (method, _, parameterRegister) = MiniPlayerOverrideNoContextFingerprint.addProxyCall()
        // - 1 means to insert before the return instruction
        val secondInsertIndex = method.implementation!!.instructions.size - 1
        method.insertOverride(secondInsertIndex, parameterRegister /** same register used to return **/)

        /*
         * method with context parameter
         */
        MiniPlayerOverrideFingerprint.resolve(context, miniPlayerClass)
        val (_, _, _) = MiniPlayerOverrideFingerprint.addProxyCall()

        /*
         * size check return value override
         */
        val (_, _, _) = MiniPlayerResponseModelSizeCheckFingerprint.addProxyCall()

        return PatchResultSuccess()
    }

    // helper methods
    private companion object {
        fun MethodFingerprint.addProxyCall(): Triple<MutableMethod, Int, Int> {
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

        fun MethodFingerprint.unwrap(): Triple<MutableMethod, Int, Int> {
            val result = this.result!!
            val scanIndex = result.scanResult.patternScanResult!!.endIndex
            val method = result.mutableMethod
            val instructions = method.implementation!!.instructions
            val parameterRegister = (instructions[scanIndex] as OneRegisterInstruction).registerA

            return Triple(method, scanIndex, parameterRegister)
        }
    }
}
