package app.revanced.patches.reddit.layout.navigation.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.layout.navigation.fingerprints.BottomNavScreenFingerprint
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.reddit.utils.settings.bytecode.patch.SettingsPatch
import app.revanced.patches.reddit.utils.settings.bytecode.patch.SettingsPatch.Companion.updateSettingsStatus
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction

@Patch
@Name("hide-navigation-buttons")
@Description("Hide navigation buttons.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
@Version("0.0.1")
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

        updateSettingsStatus("NavigationButtons")

        return PatchResultSuccess()
    }

    private companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/patches/NavigationButtonsPatch;" +
                    "->hideNavigationButtons(Ljava/util/List;)Ljava/util/List;"
    }
}
