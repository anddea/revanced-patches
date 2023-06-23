package app.revanced.patches.youtube.general.tabletminiplayer.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.general.tabletminiplayer.fingerprints.MiniPlayerDimensionsCalculatorFingerprint
import app.revanced.patches.youtube.general.tabletminiplayer.fingerprints.MiniPlayerOverrideFingerprint
import app.revanced.patches.youtube.general.tabletminiplayer.fingerprints.MiniPlayerOverrideNoContextFingerprint
import app.revanced.patches.youtube.general.tabletminiplayer.fingerprints.MiniPlayerResponseModelSizeCheckFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getStringIndex
import app.revanced.util.integrations.Constants.GENERAL
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
        MiniPlayerOverrideFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        MiniPlayerDimensionsCalculatorFingerprint.result?.let { parentResult ->
            MiniPlayerOverrideNoContextFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let { result ->
                val (method, _, parameterRegister) = result.addProxyCall()
                method.insertOverride(
                    method.implementation!!.instructions.size - 1,
                    parameterRegister
                )
            } ?: return MiniPlayerOverrideNoContextFingerprint.toErrorResult()
        } ?: return MiniPlayerDimensionsCalculatorFingerprint.toErrorResult()

        MiniPlayerOverrideFingerprint.result?.let {
            it.mutableMethod.apply {
                (context.toMethodWalker(this)
                    .nextMethod(getStringIndex("appName") + 2, true)
                    .getMethod() as MutableMethod)
                    .instructionProxyCall()
            }
        } ?: return MiniPlayerOverrideFingerprint.toErrorResult()

        MiniPlayerResponseModelSizeCheckFingerprint.result?.addProxyCall()
            ?: return MiniPlayerResponseModelSizeCheckFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: ENABLE_TABLET_MINI_PLAYER"
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
                    invoke-static {v$overrideRegister}, $GENERAL->enableTabletMiniPlayer(Z)Z
                    move-result v$overrideRegister
                    """
            )
        }

        fun MutableMethod.instructionProxyCall() {
            val insertInstructions = this.implementation!!.instructions
            for ((index, instruction) in insertInstructions.withIndex()) {
                if (instruction.opcode != Opcode.RETURN) continue
                val parameterRegister = this.getInstruction<OneRegisterInstruction>(index).registerA
                this.insertOverride(index, parameterRegister)
                this.insertOverride(insertInstructions.size - 1, parameterRegister)
                break
            }
        }

        fun MethodFingerprintResult.unwrap(): Triple<MutableMethod, Int, Int> {
            val scanIndex = this.scanResult.patternScanResult!!.endIndex
            val method = this.mutableMethod
            val parameterRegister =
                method.getInstruction<OneRegisterInstruction>(scanIndex).registerA

            return Triple(method, scanIndex, parameterRegister)
        }
    }
}
