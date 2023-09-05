package app.revanced.patches.youtube.utils.settings.bytecode.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.patch.mapping.ResourceMappingPatch
import app.revanced.patches.youtube.utils.integrations.patch.IntegrationsPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.bytecode.fingerprints.ThemeSetterSystemFingerprint
import app.revanced.util.bytecode.BytecodeHelper.injectInit
import app.revanced.util.integrations.Constants.INTEGRATIONS_PATH

@DependsOn(
    [
        IntegrationsPatch::class,
        ResourceMappingPatch::class,
        SharedResourceIdPatch::class
    ]
)
class SettingsBytecodePatch : BytecodePatch(
    listOf(ThemeSetterSystemFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        // apply the current theme of the settings page
        ThemeSetterSystemFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                replaceInstruction(
                    targetIndex,
                    SET_THEME
                )
                addInstruction(
                    targetIndex + 1,
                    "return-object v0"
                )
                addInstruction(
                    this.implementation!!.instructions.size - 1,
                    SET_THEME
                )
            }
        } ?: throw ThemeSetterSystemFingerprint.exception

        context.injectInit("FirstRun", "initializationRVX", true)

    }

    companion object {
        const val SET_THEME =
            "invoke-static {v0}, $INTEGRATIONS_PATH/utils/ThemeHelper;->setTheme(Ljava/lang/Object;)V"
    }
}
