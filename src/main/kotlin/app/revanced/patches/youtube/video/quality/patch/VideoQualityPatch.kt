package app.revanced.patches.youtube.video.quality.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.NewFlyoutPanelOnClickListenerFingerprint
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch.Companion.contexts
import app.revanced.patches.youtube.utils.videoid.withoutshorts.patch.VideoIdWithoutShortsPatch
import app.revanced.patches.youtube.video.quality.fingerprints.NewVideoQualityChangedFingerprint
import app.revanced.patches.youtube.video.quality.fingerprints.VideoQualityReferenceFingerprint
import app.revanced.patches.youtube.video.quality.fingerprints.VideoQualitySetterFingerprint
import app.revanced.patches.youtube.video.quality.fingerprints.VideoQualitySettingsFingerprint
import app.revanced.patches.youtube.video.quality.fingerprints.VideoQualitySettingsParentFingerprint
import app.revanced.patches.youtube.video.quality.fingerprints.VideoUserQualityChangeFingerprint
import app.revanced.util.integrations.Constants.VIDEO_PATH
import app.revanced.util.resources.ResourceUtils.copyXmlNode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Patch
@Name("Default video quality")
@Description("Adds ability to set default video quality settings.")
@DependsOn(
    [
        VideoIdWithoutShortsPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
class VideoQualityPatch : BytecodePatch(
    listOf(
        NewFlyoutPanelOnClickListenerFingerprint,
        VideoQualitySetterFingerprint,
        VideoQualitySettingsParentFingerprint
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
                        "invoke-static {v$register}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->userChangedQualityInNewFlyoutPanels(I)V"
                    )
                }
            }
        } ?: throw NewFlyoutPanelOnClickListenerFingerprint.exception

        VideoQualitySetterFingerprint.result?.let { parentResult ->
            VideoQualityReferenceFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let { result ->
                result.mutableMethod.apply {
                    qualityFieldReference =
                        getInstruction<ReferenceInstruction>(0).reference as FieldReference

                    qIndexMethodName = context.classes
                        .single { it.type == qualityFieldReference.type }.methods
                        .single { it.parameterTypes.first() == "I" }.name
                }
            } ?: throw VideoQualityReferenceFingerprint.exception

            VideoUserQualityChangeFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.mutableMethod?.addInstruction(
                0,
                "invoke-static {p3}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->userChangedQuality(I)V"
            ) ?: throw VideoUserQualityChangeFingerprint.exception
        } ?: throw VideoQualitySetterFingerprint.exception

        VideoQualitySettingsParentFingerprint.result?.let { parentResult ->
            VideoQualitySettingsFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.mutableMethod?.let {
                relayFieldReference =
                    it.getInstruction<ReferenceInstruction>(0).reference as FieldReference
            } ?: throw VideoQualitySettingsFingerprint.exception

            parentResult.mutableMethod.addInstructions(
                0, """
                    iget-object v0, p0, ${parentResult.classDef.type}->${relayFieldReference.name}:${relayFieldReference.type}
                    iget-object v1, v0, ${relayFieldReference.type}->${qualityFieldReference.name}:${qualityFieldReference.type}
                    const-string v2, "$qIndexMethodName"
                    invoke-static {p1, p2, v1, v2}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->setVideoQuality([Ljava/lang/Object;ILjava/lang/Object;Ljava/lang/String;)I
                    move-result p2
                    """
            )
        } ?: throw VideoQualitySettingsParentFingerprint.exception

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

        private lateinit var qIndexMethodName: String

        private lateinit var relayFieldReference: FieldReference
        private lateinit var qualityFieldReference: FieldReference
    }
}