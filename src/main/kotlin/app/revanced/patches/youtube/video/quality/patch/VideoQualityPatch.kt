package app.revanced.patches.youtube.video.quality.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.NewFlyoutPanelOnClickListenerFingerprint
import app.revanced.patches.youtube.utils.overridequality.patch.OverrideQualityHookPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch.Companion.contexts
import app.revanced.patches.youtube.utils.videoid.general.patch.VideoIdPatch
import app.revanced.patches.youtube.utils.videoid.withoutshorts.patch.VideoIdWithoutShortsPatch
import app.revanced.patches.youtube.video.quality.fingerprints.NewVideoQualityChangedFingerprint
import app.revanced.patches.youtube.video.quality.fingerprints.VideoQualitySetterFingerprint
import app.revanced.util.integrations.Constants.VIDEO_PATH
import app.revanced.util.resources.ResourceUtils.copyXmlNode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("Default video quality")
@Description("Adds ability to set default video quality settings.")
@DependsOn(
    [
        OverrideQualityHookPatch::class,
        VideoIdPatch::class,
        VideoIdWithoutShortsPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
class VideoQualityPatch : BytecodePatch(
    listOf(
        NewFlyoutPanelOnClickListenerFingerprint,
        VideoQualitySetterFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        NewFlyoutPanelOnClickListenerFingerprint.result?.let { parentResult ->
            NewVideoQualityChangedFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val index = it.scanResult.patternScanResult!!.endIndex
                    val register = getInstruction<TwoRegisterInstruction>(index).registerA

                    addInstruction(
                        index + 1,
                        "invoke-static {v$register}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->userChangedQuality(I)V"
                    )
                }
            }
        } ?: throw NewFlyoutPanelOnClickListenerFingerprint.exception

        VideoQualitySetterFingerprint.result?.let {
            val onItemClickMethod = it.mutableClass.methods.find { method -> method.name == "onItemClick" }

            onItemClickMethod?.apply {
                val listItemIndexParameter = 3

                addInstruction(
                    0,
                    "invoke-static {p$listItemIndexParameter}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->userChangedQualityIndex(I)V"
                )
            } ?: throw PatchException("Failed to find onItemClick method")
        } ?: throw VideoQualitySetterFingerprint.exception

        VideoIdPatch.injectCall("$INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;)V")
        VideoIdWithoutShortsPatch.injectCall("$INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;)V")

        /**
         * Copy arrays
         */
        contexts.copyXmlNode("youtube/quality/host", "values/arrays.xml", "resources")


        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: VIDEO_SETTINGS",
                "SETTINGS: DEFAULT_VIDEO_QUALITY"
            )
        )

        SettingsPatch.updatePatchStatus("default-video-quality")

    }

    private companion object {
        const val INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR =
            "$VIDEO_PATH/VideoQualityPatch;"
    }
}