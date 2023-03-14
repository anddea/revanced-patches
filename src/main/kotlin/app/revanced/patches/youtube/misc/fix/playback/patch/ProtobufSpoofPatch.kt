package app.revanced.patches.youtube.misc.fix.playback.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.fix.playback.fingerprints.ProtobufParameterBuilderFingerprint

@Patch
@Name("protobuf-spoof")
@Description("Spoofs the protobuf to prevent playback issues.")
@YouTubeCompatibility
@Version("0.0.1")
class ProtobufSpoofPatch : BytecodePatch(
    listOf(ProtobufParameterBuilderFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        ProtobufParameterBuilderFingerprint.result?.let {
            with (context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.startIndex, true)
                .getMethod() as MutableMethod
            ) {
                val protobufParam = 3
                val protobufParameter = "8AEByAMTuAQP" /* Protobuf Parameter of shorts */

                addInstruction(
                    0,
                    "const-string p$protobufParam, \"$protobufParameter\""
                )
            }
        } ?: return ProtobufParameterBuilderFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
