package app.revanced.patches.reddit.layout.navigation.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenFingerprint
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction

class NavigationButtonsPatch : BytecodePatch(
    listOf(BottomNavScreenFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        BottomNavScreenFingerprint.result?.let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val reference =
                    getInstruction<ReferenceInstruction>(startIndex).reference.toString()

                if (!reference.endsWith("Ljava/util/List;"))
                    return PatchResultError("Invalid reference: $reference")

                val insertIndex = startIndex + 2
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(startIndex + 1).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $INTEGRATIONS_METHOD_DESCRIPTOR
                        move-result-object v$insertRegister
                        """
                )
            }
        } ?: return BottomNavScreenFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    companion object {
        const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/patches/NavigationButtonsPatch;" +
                    "->hideNavigationButtons(Ljava/util/List;)Ljava/util/List;"

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
