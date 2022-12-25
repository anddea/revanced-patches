package app.revanced.patches.youtube.video.speed.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.youtube.video.speed.bytecode.fingerprints.VideoSpeedSetterFingerprint
import app.revanced.patches.youtube.video.speed.bytecode.fingerprints.VideoUserSpeedChangeFingerprint
import app.revanced.patches.youtube.misc.videoid.legacy.patch.LegacyVideoIdPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.bytecode.BytecodeHelper
import app.revanced.shared.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodImplementation
import org.jf.dexlib2.immutable.ImmutableMethodParameter

@Name("default-video-speed-bytecode-patch")
@DependsOn([LegacyVideoIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class VideoSpeedBytecodePatch : BytecodePatch(
    listOf(
        VideoSpeedSetterFingerprint, VideoUserSpeedChangeFingerprint
    )
) {
    private companion object {
        const val INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR =
            "$VIDEO_PATH/VideoSpeedPatch;"
    }
    override fun execute(context: BytecodeContext): PatchResult {

        val userSpeedResult = VideoUserSpeedChangeFingerprint.result!!
        val userSpeedMutableMethod = userSpeedResult.mutableMethod

        val setterResult = VideoSpeedSetterFingerprint.result!!
        val setterMutableMethod = setterResult.mutableMethod

        VideoUserSpeedChangeFingerprint.resolve(context, setterResult.classDef)
        val FirstReference =
            VideoUserSpeedChangeFingerprint.result!!.method.let { method ->
                (method.implementation!!.instructions.elementAt(5) as ReferenceInstruction).reference as FieldReference
            }
        val SecondReference =
            VideoUserSpeedChangeFingerprint.result!!.method.let { method ->
                (method.implementation!!.instructions.elementAt(10) as ReferenceInstruction).reference as FieldReference
            }
        val ThirdReference =
            VideoUserSpeedChangeFingerprint.result!!.method.let { method ->
                (method.implementation!!.instructions.elementAt(11) as ReferenceInstruction).reference as MethodReference
            }

        userSpeedMutableMethod.addInstruction(
            0, "invoke-static {}, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->userChangedSpeed()V"
        )

        setterMutableMethod.addInstructions(
            0,
            """
                   invoke-static {p1, p2}, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->getSpeedValue([Ljava/lang/Object;I)F
                   move-result v0
                   invoke-direct {p0, v0}, ${setterResult.classDef.type}->overrideSpeed(F)V
            """,
        )

        val classDef = userSpeedResult.mutableClass 
        classDef.methods.add(
            ImmutableMethod(
                classDef.type,
                "overrideSpeed",
                listOf(ImmutableMethodParameter("F", null, null)),
                "V",
                AccessFlags.PRIVATE or AccessFlags.PRIVATE,
                null,
                null,
                ImmutableMethodImplementation(
                    4, """
                        const/4 v0, 0x0
                        cmpg-float v0, v3, v0
                        if-gez v0, :cond_0
                        return-void
                        :cond_0
                        iget-object v0, v2, ${setterResult.classDef.type}->${FirstReference.name}:${FirstReference.type}
                        check-cast v0, ${SecondReference.definingClass}
                        iget-object v1, v0, ${SecondReference.definingClass}->${SecondReference.name}:${SecondReference.type}
                        invoke-virtual {v1, v3}, $ThirdReference
                        return-void
                    """.toInstructions(), null, null
                )
            ).toMutable()
        )

        LegacyVideoIdPatch.injectCall("$INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;)V")

        BytecodeHelper.patchStatus(context, "VideoSpeed")

        return PatchResultSuccess()
    }
}