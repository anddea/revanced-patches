package app.revanced.patches.youtube.layout.general.tabletminiplayer.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.tabletminiplayer.fingerprints.*
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL_LAYOUT
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("enable-tablet-miniplayer")
@Description("Enables the tablet mini player layout.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class TabletMiniPlayerPatch : BytecodePatch(
    listOf(
        MiniPlayerDimensionsCalculatorFingerprint,
        MiniPlayerResponseModelSizeCheckFingerprint,
        MiniPlayerOverrideParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        MiniPlayerDimensionsCalculatorFingerprint.result?.let { parentResult ->
            MiniPlayerOverrideNoContextFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let { result ->
                val (method, _, parameterRegister) = result.addProxyCall()
                method.insertOverride(method.implementation!!.instructions.size - 1, parameterRegister)
            } ?: return MiniPlayerOverrideNoContextFingerprint.toErrorResult()
        } ?: return MiniPlayerDimensionsCalculatorFingerprint.toErrorResult()

        MiniPlayerOverrideParentFingerprint.result?.let { parentResult ->
            MiniPlayerOverrideFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let { result ->
                (context.toMethodWalker(result.method)
                        .nextMethod(result.scanResult.patternScanResult!!.startIndex, true)
                        .getMethod() as MutableMethod)
                .instructionProxyCall()
            } ?: return MiniPlayerOverrideFingerprint.toErrorResult()

        } ?: return MiniPlayerOverrideParentFingerprint.toErrorResult()

        MiniPlayerResponseModelSizeCheckFingerprint.result?.let {
            val (_, _, _) = it.addProxyCall()
        } ?: return MiniPlayerResponseModelSizeCheckFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_LAYOUT_SETTINGS",
                "SETTINGS: ENABLE_TABLET_MINIPLAYER"
            )
        )

        SettingsPatch.updatePatchStatus("enable-tablet-miniplayer")

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

        fun MutableMethod.instructionProxyCall() {
            val insertInstructions = this.implementation!!.instructions
            for ((index, instruction) in insertInstructions.withIndex()) {
                if (instruction.opcode != Opcode.RETURN) continue
                val parameterRegister = (instruction as OneRegisterInstruction).registerA
                this.insertOverride(index, parameterRegister)
                this.insertOverride(insertInstructions.size - 1, parameterRegister)
                break
            }
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
