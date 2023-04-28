package app.revanced.patches.music.misc.quality.patch

import app.revanced.extensions.findMutableMethodOf
import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.misc.quality.fingerprints.*
import app.revanced.patches.music.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.misc.settings.resource.patch.MusicSettingsPatch
import app.revanced.patches.music.misc.videoid.patch.MusicVideoIdPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_MISC_PATH
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference

@Patch
@Name("remember-video-quality")
@Description("Save the video quality value whenever you change the video quality.")
@DependsOn(
    [
        MusicSettingsPatch::class,
        MusicVideoIdPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeMusicCompatibility
@Version("0.0.1")
class VideoQualityPatch : BytecodePatch(
    listOf(
        MusicVideoQualitySetterParentFingerprint,
        MusicVideoQualitySettingsParentFingerprint,
        UserQualityChangeFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        MusicVideoQualitySetterParentFingerprint.result?.let { parentResult ->
            MusicVideoQualitySetterFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let { result ->
                val endIndex = result.scanResult.patternScanResult!!.endIndex
                val instructions = result.method.implementation!!.instructions

                qualityFieldReference =
                    (instructions.elementAt(endIndex) as ReferenceInstruction).reference as FieldReference

                qIndexMethodName =
                    context.classes.single { it.type == qualityFieldReference.type }.methods.single { it.parameterTypes.first() == "I" }.name
            } ?: return MusicVideoQualitySetterFingerprint.toErrorResult()
        } ?: return MusicVideoQualitySetterParentFingerprint.toErrorResult()

        MusicVideoQualitySettingsParentFingerprint.result?.let { parentResult ->
            MusicVideoQualitySettingsFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                it.mutableMethod.addInstructions(
                    0, """
                        const-string v0, "$qIndexMethodName"
                        sput-object v0, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->qIndexMethod:Ljava/lang/String;
                        iget-object v0, p0, ${it.classDef.type}->${qualityFieldReference.name}:${qualityFieldReference.type}
                        invoke-static {p1, p2, v0}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->setVideoQuality([Ljava/lang/Object;ILjava/lang/Object;)I
                        move-result p2
                     """
                )
            } ?: return MusicVideoQualitySettingsFingerprint.toErrorResult()
        } ?: return MusicVideoQualitySettingsParentFingerprint.toErrorResult()

        UserQualityChangeFingerprint.result?.let {
            val endIndex = it.scanResult.patternScanResult!!.endIndex
            val qualityChangedClass =
                context.findClass((it.mutableMethod.instruction(endIndex) as BuilderInstruction21c)
                    .reference.toString())!!
                    .mutableClass

            for (method in qualityChangedClass.methods) {
                with (qualityChangedClass.findMutableMethodOf(method)) {
                    if (this.name == "onItemClick") {
                        addInstruction(
                            0,
                            "invoke-static {p3}, $INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->userChangedQuality(I)V"
                        )
                    }
                }
            }
        } ?: return UserQualityChangeFingerprint.toErrorResult()

        MusicVideoIdPatch.injectCall("$INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;)V")
        MusicSettingsPatch.addMusicPreference(CategoryType.MISC, "revanced_enable_save_video_quality", "true")

        return PatchResultSuccess()
    }
    private companion object {
        const val INTEGRATIONS_VIDEO_QUALITY_CLASS_DESCRIPTOR =
            "$MUSIC_MISC_PATH/MusicVideoQualityPatch;"

        private lateinit var qIndexMethodName: String
        private lateinit var qualityFieldReference: FieldReference

    }
}
