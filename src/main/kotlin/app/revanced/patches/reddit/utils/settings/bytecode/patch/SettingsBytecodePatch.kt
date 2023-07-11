package app.revanced.patches.reddit.utils.settings.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.reddit.utils.settings.bytecode.fingerprints.OssLicensesMenuActivityOnCreateFingerprint
import app.revanced.patches.reddit.utils.settings.bytecode.fingerprints.SettingsStatusLoadFingerprint
import kotlin.properties.Delegates

class SettingsBytecodePatch : BytecodePatch(
    listOf(
        OssLicensesMenuActivityOnCreateFingerprint,
        SettingsStatusLoadFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        OssLicensesMenuActivityOnCreateFingerprint.result?.let {
            activityMethod = it.mutableMethod
            activityIndex = it.scanResult.patternScanResult!!.startIndex + 1
        } ?: return OssLicensesMenuActivityOnCreateFingerprint.toErrorResult()

        settingsMethod = SettingsStatusLoadFingerprint.result?.mutableMethod
            ?: return SettingsStatusLoadFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    internal companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/settingsmenu/ReVancedSettingActivity;->initializeSettings(Landroid/app/Activity;)V"

        private lateinit var activityMethod: MutableMethod
        private var activityIndex by Delegates.notNull<Int>()
        private lateinit var settingsMethod: MutableMethod

        fun injectActivity() {
            activityMethod.apply {
                addInstructions(
                    activityIndex, """
                        invoke-static {p0}, $INTEGRATIONS_METHOD_DESCRIPTOR
                        return-void
                        """
                )
            }
        }

        fun updateSettingsStatus(description: String) {
            settingsMethod.apply {
                addInstruction(
                    0,
                    "invoke-static {}, Lapp/revanced/reddit/settingsmenu/SettingsStatus;->$description()V"
                )
            }
        }
    }
}