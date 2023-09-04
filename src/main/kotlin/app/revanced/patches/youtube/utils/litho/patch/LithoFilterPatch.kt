package app.revanced.patches.youtube.utils.litho.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.patch.litho.ComponentParserPatch
import app.revanced.patches.shared.patch.litho.ComponentParserPatch.Companion.generalHook
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.litho.fingerprints.GeneralByteBufferFingerprint
import app.revanced.patches.youtube.utils.litho.fingerprints.LithoFilterFingerprint
import app.revanced.patches.youtube.utils.litho.fingerprints.LowLevelByteBufferFingerprint
import app.revanced.patches.youtube.utils.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.ADS_PATH
import java.io.Closeable

@DependsOn(
    [
        ComponentParserPatch::class,
        PlayerTypeHookPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
class LithoFilterPatch : BytecodePatch(
    listOf(
        GeneralByteBufferFingerprint,
        LithoFilterFingerprint,
        LowLevelByteBufferFingerprint
    )
), Closeable {
    override fun execute(context: BytecodeContext) {


        LowLevelByteBufferFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "invoke-static { p0 }, $ADS_PATH/LowLevelFilter;->setProtoBuffer(Ljava/nio/ByteBuffer;)V"
        ) ?: throw LowLevelByteBufferFingerprint.exception

        GeneralByteBufferFingerprint.result?.let {
            (context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.endIndex, true)
                .getMethod() as MutableMethod
                    ).apply {
                    addInstruction(
                        0,
                        "invoke-static { p2 }, $ADS_PATH/LithoFilterPatch;->setProtoBuffer(Ljava/nio/ByteBuffer;)V"
                    )
                }
        } ?: throw GeneralByteBufferFingerprint.exception

        generalHook("$ADS_PATH/LithoFilterPatch;->filters")

        LithoFilterFingerprint.result?.mutableMethod?.apply {
            removeInstructions(2, 4) // Remove dummy filter.

            addFilter = { classDescriptor ->
                addInstructions(
                    2, """
                        new-instance v1, $classDescriptor
                        invoke-direct {v1}, $classDescriptor-><init>()V
                        ${getConstString(2, filterCount++)}
                        aput-object v1, v0, v2
                        """
                )
            }
        } ?: throw LithoFilterFingerprint.exception

    }

    override fun close() = LithoFilterFingerprint.result!!
        .mutableMethod.replaceInstruction(0, getConstString(0, filterCount))

    companion object {
        internal lateinit var addFilter: (String) -> Unit
            private set

        private var filterCount = 0

        private fun getConstString(
            register: Int,
            count: Int
        ): String = if (count >= 8) "const/16 v$register, $count" else "const/4 v$register, $count"
    }
}
