package app.revanced.patches.youtube.layout.fullscreen.landscapemode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.fullscreen.landscapemode.fingerprints.*
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.FULLSCREEN
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(false)
@Name("diable-landscape-mode")
@Description("Disable landscape mode when entering fullscreen.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class LandScapeModePatch : BytecodePatch(
    listOf(
        OrientationParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        OrientationParentFingerprint.result?.classDef?.let { classDef ->
            arrayOf(
                OrientationPrimaryFingerprint,
                OrientationSecondaryFingerprint
            ).forEach { fingerprint ->
                fingerprint.also { it.resolve(context, classDef) }.result?.injectOverride() ?: return fingerprint.toErrorResult()
            }
        } ?: return OrientationParentFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: DISABLE_LANDSCAPE_MODE"
            )
        )

        SettingsPatch.updatePatchStatus("diable-landscape-mode")

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$FULLSCREEN->disableLandScapeMode(Z)Z"

        fun MethodFingerprintResult.injectOverride() {
            with (mutableMethod) {
                val index = scanResult.patternScanResult!!.endIndex
                val register = (instruction(index) as OneRegisterInstruction).registerA

                addInstructions(
                    index +1, """
                        invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR
                        move-result v$register
                    """
                )
            }
        }
    }
}
