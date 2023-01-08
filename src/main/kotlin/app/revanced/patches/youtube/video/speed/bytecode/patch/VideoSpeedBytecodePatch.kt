package app.revanced.patches.youtube.video.speed.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.youtube.misc.videoid.legacy.patch.LegacyVideoIdPatch
import app.revanced.patches.youtube.video.speed.bytecode.fingerprints.VideoSpeedChangedFingerprint
import app.revanced.patches.youtube.video.speed.bytecode.fingerprints.VideoSpeedParentFingerprint
import app.revanced.patches.youtube.video.speed.bytecode.fingerprints.VideoSpeedSetterFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.toErrorResult
import app.revanced.shared.util.bytecode.BytecodeHelper
import app.revanced.shared.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodImplementation
import org.jf.dexlib2.immutable.ImmutableMethodParameter

@Name("default-video-speed-bytecode-patch")
@DependsOn([LegacyVideoIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class VideoSpeedBytecodePatch : BytecodePatch(
    listOf(
        VideoSpeedChangedFingerprint,
        VideoSpeedParentFingerprint,
        VideoSpeedSetterFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        VideoSpeedParentFingerprint.result?.let { parentResult ->
            val parentClassDef = parentResult.classDef

            VideoSpeedChangedFingerprint.also { it.resolve(context, parentClassDef) }.result?.let {
                startIndex = it.scanResult.patternScanResult!!.startIndex
                endIndex = it.scanResult.patternScanResult!!.endIndex
                
                with (it.method) {
                    val speedInstruction = implementation!!.instructions

                    firstRef = 
                        (speedInstruction.elementAt(startIndex) as ReferenceInstruction).reference as FieldReference

                    secondRef = 
                        (speedInstruction.elementAt(endIndex - 1) as ReferenceInstruction).reference as FieldReference

                    thirdRef = 
                        (speedInstruction.elementAt(endIndex) as ReferenceInstruction).reference as DexBackedMethodReference

                    val register =
                        (speedInstruction.elementAt(endIndex) as FiveRegisterInstruction).registerD

                    it.mutableMethod.addInstruction(
                        endIndex,
                        "invoke-static { v$register }, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR" +
                        "->" +
                        "userChangedSpeed(F)V"
                    )
                }

            } ?: return VideoSpeedChangedFingerprint.toErrorResult()

            val parentMutableClass = parentResult.mutableClass

            parentMutableClass.methods.add(
                ImmutableMethod(
                    parentMutableClass.type,
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
                            if-lez v0, :cond_0
                            iget-object v0, v2, ${parentClassDef.type}->${firstRef.name}:${firstRef.type}
                            check-cast v0, ${secondRef.definingClass}
                            iget-object v1, v0, ${secondRef.definingClass}->${secondRef.name}:${secondRef.type}
                            invoke-virtual {v1, v3}, $thirdRef
                            :cond_0
                            return-void
                        """.toInstructions(), null, null
                    )
                ).toMutable()
            )

        } ?: return VideoSpeedParentFingerprint.toErrorResult()

        VideoSpeedSetterFingerprint.result?.let {
            it.mutableMethod.addInstructions(
                    0, """
                        invoke-static {}, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->getSpeedValue()F
                        move-result v0
                        invoke-direct {p0, v0}, ${it.classDef.type}->overrideSpeed(F)V
                    """,
                )
        } ?: return VideoSpeedSetterFingerprint.toErrorResult()

        LegacyVideoIdPatch.injectCall("$INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;)V")

        BytecodeHelper.patchStatus(context, "VideoSpeed")

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR =
            "$VIDEO_PATH/VideoSpeedPatch;"

        var startIndex: Int = 0
        var endIndex: Int = 0

        private lateinit var firstRef: FieldReference
        private lateinit var secondRef: FieldReference
        private lateinit var thirdRef: DexBackedMethodReference
    }
}