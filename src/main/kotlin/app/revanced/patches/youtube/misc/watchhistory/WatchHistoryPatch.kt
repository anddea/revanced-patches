package app.revanced.patches.youtube.misc.watchhistory

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.misc.watchhistory.fingerprints.TrackingUrlModelFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
object WatchHistoryPatch : BaseBytecodePatch(
    name = "Spoof watch history",
    description = "Adds an option to change the domain of the watch history or check its status.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(TrackingUrlModelFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        TrackingUrlModelFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $MISC_PATH/WatchHistoryPatch;->replaceTrackingUrl(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
                )
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: WATCH_HISTORY"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
