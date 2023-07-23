package app.revanced.patches.reddit.layout.navigation.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenFingerprint
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction

class NavigationButtonsPatch : BytecodePatch(
    listOf(BottomNavScreenFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        BottomNavScreenFingerprint.result?.let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegister =
                    getInstruction<FiveRegisterInstruction>(startIndex).registerC

                addInstruction(
                    startIndex + 1,
                        "invoke-static {v$targetRegister}, $INTEGRATIONS_METHOD_DESCRIPTOR"
                )
            }
        } ?: return BottomNavScreenFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    companion object {
        const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/patches/NavigationButtonsPatch;" +
                    "->hideNavigationButtons(Landroid/view/ViewGroup;)V"

        internal fun BytecodeContext.setValue(patch: String) {
            this.classes.forEach { classDef ->
                classDef.methods.forEach { method ->
                    if (classDef.type == "Lapp/revanced/reddit/settingsmenu/SettingsStatus;" && method.name == patch) {
                        val patchStatusMethod =
                            this.proxy(classDef).mutableClass.methods.first { it.name == patch }

                        patchStatusMethod.addInstruction(
                            2,
                            "return-void"
                        )
                    }
                }
            }
        }
    }
}
