package app.revanced.patches.youtube.video.restrictions.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.video.restrictions.fingerprints.*
import app.revanced.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("lift-vertical-video-restriction")
@Description("Lift 4K resolution restrictions on vertical video.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class VideoRestrictionPatch : BytecodePatch(
    listOf(
        VideoCapabilitiesParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        VideoCapabilitiesParentFingerprint.result?.let { parentResult ->
            VideoCapabilitiesFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val endIndex = it.scanResult.patternScanResult!!.endIndex

                for (index in endIndex downTo startIndex)
                    it.mutableMethod.hookOverride(index)
            } ?: return VideoCapabilitiesFingerprint.toErrorResult()
        } ?: return VideoCapabilitiesParentFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: VERTICAL_VIDEO_RESTRICTIONS"
            )
        )

        SettingsPatch.updatePatchStatus("lift-vertical-video-restriction")

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$VIDEO_PATH/VideoRestrictionPatch;"

        const val INTEGRATIONS_CLASS_METHOD_LOWER_REFERENCE =
            "$INTEGRATIONS_CLASS_DESCRIPTOR->overrideLowerRange(I)I"

        const val INTEGRATIONS_CLASS_METHOD_UPPER_REFERENCE =
            "$INTEGRATIONS_CLASS_DESCRIPTOR->overrideUpperRange(I)I"

        fun MutableMethod.hookOverride(index: Int) {
            val register = (instruction(index) as TwoRegisterInstruction).registerA
            val descriptor =
                if (index % 2 == 0) INTEGRATIONS_CLASS_METHOD_UPPER_REFERENCE
                else INTEGRATIONS_CLASS_METHOD_LOWER_REFERENCE

            addInstructions(
                index, """
                    invoke-static {v$register}, $descriptor
                    move-result v$register
                """
            )
        }
    }

}
