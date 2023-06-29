package app.revanced.patches.music.utils.litho.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstructions
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.litho.fingerprints.LithoFilterFingerprint
import app.revanced.patches.shared.patch.litho.ComponentParserPatch
import app.revanced.patches.shared.patch.litho.ComponentParserPatch.Companion.identifierHook
import app.revanced.util.integrations.Constants.MUSIC_ADS_PATH
import java.io.Closeable

@DependsOn([ComponentParserPatch::class])
@MusicCompatibility
@Version("0.0.1")
class LithoFilterPatch : BytecodePatch(
    listOf(LithoFilterFingerprint)
), Closeable {
    override fun execute(context: BytecodeContext): PatchResult {
        identifierHook("$MUSIC_ADS_PATH/LithoFilterPatch;->filter")

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
