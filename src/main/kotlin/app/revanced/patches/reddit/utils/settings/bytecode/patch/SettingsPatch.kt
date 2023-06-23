package app.revanced.patches.reddit.utils.settings.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.reddit.utils.integrations.patch.IntegrationsPatch
import app.revanced.patches.reddit.utils.settings.bytecode.fingerprints.OssLicensesMenuActivityOnCreateFingerprint
import app.revanced.patches.reddit.utils.settings.bytecode.fingerprints.SettingsStatusLoadFingerprint
import app.revanced.patches.reddit.utils.settings.resource.patch.SettingsResourcePatch

@Patch
@Name("settings")
@Description("Adds ReVanced settings to Reddit.")
@DependsOn(
    [
        IntegrationsPatch::class,
        SettingsResourcePatch::class
    ]
)
@RedditCompatibility
@Version("0.0.1")
class SettingsPatch : BytecodePatch(
    listOf(
        OssLicensesMenuActivityOnCreateFingerprint,
        SettingsStatusLoadFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        OssLicensesMenuActivityOnCreateFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex + 1

                addInstructions(
                    insertIndex, """
                        invoke-static {p0}, $INTEGRATIONS_METHOD_DESCRIPTOR
                        return-void
                        """
                )
            }
        } ?: return OssLicensesMenuActivityOnCreateFingerprint.toErrorResult()

        targetMethod = SettingsStatusLoadFingerprint.result?.mutableMethod
            ?: return SettingsStatusLoadFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    internal companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/settingsmenu/ReVancedSettingActivity;->initializeSettings(Landroid/app/Activity;)V"

        private lateinit var targetMethod: MutableMethod

        fun updateSettingsStatus(description: String) {
            targetMethod.apply {
                addInstruction(
                    0,
                    "invoke-static {}, Lapp/revanced/reddit/settingsmenu/SettingsStatus;->$description()V"
                )
            }
        }
    }
}