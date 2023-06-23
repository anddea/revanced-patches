package app.revanced.patches.youtube.utils.litho.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.patch.litho.ComponentParserPatch
import app.revanced.patches.shared.patch.litho.ComponentParserPatch.Companion.generalHook
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.litho.fingerprints.ByteBufferFingerprint
import app.revanced.patches.youtube.utils.litho.fingerprints.LithoFilterFingerprint
import app.revanced.patches.youtube.utils.playertype.patch.PlayerTypeHookPatch
import app.revanced.util.integrations.Constants.ADS_PATH
import java.io.Closeable

@DependsOn(
    [
        ComponentParserPatch::class,
        PlayerTypeHookPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class LithoFilterPatch : BytecodePatch(
    listOf(
        ByteBufferFingerprint,
        LithoFilterFingerprint
    )
), Closeable {
    override fun execute(context: BytecodeContext): PatchResult {


        ByteBufferFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "sput-object p0, $ADS_PATH/ByteBufferFilterPatch;->bytebuffer:Ljava/nio/ByteBuffer;"
        ) ?: return ByteBufferFingerprint.toErrorResult()

        generalHook("$ADS_PATH/LithoFilterPatch;->filters")

        LithoFilterFingerprint.result?.mutableMethod?.apply {
            removeInstructions(2, 4) // Remove dummy filter.

            addFilter = { classDescriptor ->
                addInstructions(
                    2,
                    """
                        new-instance v1, $classDescriptor
                        invoke-direct {v1}, $classDescriptor-><init>()V
                        const/4 v2, ${filterCount++}
                        aput-object v1, v0, v2
                    """
                )
            }
        } ?: return LithoFilterFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    override fun close() = LithoFilterFingerprint.result!!
        .mutableMethod.replaceInstruction(0, "const/4 v0, $filterCount")

    companion object {
        internal lateinit var addFilter: (String) -> Unit
            private set

        private var filterCount = 0
    }
}
