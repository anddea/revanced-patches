package app.revanced.patches.music.video.quality

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.overridequality.OverrideQualityHookPatch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.video.information.VideoInformationPatch
import app.revanced.patches.music.video.quality.fingerprints.UserQualityChangeFingerprint
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_VIDEO_PATH
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c

@Patch(
    name = "Remember video quality",
    description = "Save the video quality value whenever you change the video quality.",
    dependencies = [
        OverrideQualityHookPatch::class,
        SettingsPatch::class,
        VideoInformationPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.27.54",
                "6.28.52"
            ]
        )
    ]
)
@Suppress("unused")
object VideoQualityPatch : BytecodePatch(
    setOf(UserQualityChangeFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        UserQualityChangeFingerprint.result?.let {
            it.mutableMethod.apply {
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val qualityChangedClass =
                    context.findClass(
                        (getInstruction<BuilderInstruction21c>(endIndex))
                            .reference.toString()
                    )!!
                        .mutableClass

                val onItemClickMethod =
                    qualityChangedClass.methods.find { method -> method.name == "onItemClick" }

                onItemClickMethod?.apply {
                    val listItemIndexParameter = 3

                    addInstruction(
                        0,
                        "invoke-static {p$listItemIndexParameter}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->userChangedQuality(I)V"
                    )
                } ?: throw PatchException("Failed to find onItemClick method")
            }
        } ?: throw UserQualityChangeFingerprint.exception

        VideoInformationPatch.injectCall("$INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;)V")

        SettingsPatch.addMusicPreference(
            CategoryType.VIDEO,
            "revanced_enable_save_video_quality",
            "true"
        )

    }

    private const val INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR =
        "$MUSIC_VIDEO_PATH/VideoQualityPatch;"
}
