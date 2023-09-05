package app.revanced.patches.music.utils.litho.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.litho.fingerprints.LithoFilterFingerprint
import app.revanced.patches.shared.patch.litho.ComponentParserPatch
import app.revanced.patches.shared.patch.litho.ComponentParserPatch.Companion.identifierHook
import app.revanced.util.integrations.Constants.MUSIC_ADS_PATH
import java.io.Closeable

@DependsOn([ComponentParserPatch::class])
@MusicCompatibility
class LithoFilterPatch : BytecodePatch(
    listOf(LithoFilterFingerprint)
), Closeable {
    override fun execute(context: BytecodeContext) {
        identifierHook("$MUSIC_ADS_PATH/LithoFilterPatch;->filter")

        LithoFilterFingerprint.result?.let {
            it.mutableMethod.apply {
                removeInstructions(0, 6)

                addFilter = { classDescriptor ->
                    addInstructions(
                        0, """
                        new-instance v0, $classDescriptor
                        invoke-direct {v0}, $classDescriptor-><init>()V
                        const/16 v2, ${filterCount++}
                        aput-object v0, v1, v2
                        """
                    )
                }
            }
        } ?: throw LithoFilterFingerprint.exception

    }

    override fun close() = LithoFilterFingerprint.result!!
        .mutableMethod.addInstructions(
            0, """
                const/16 v1, $filterCount
                new-array v1, v1, [Lapp/revanced/music/patches/ads/Filter;
                """
        )

    companion object {
        internal lateinit var addFilter: (String) -> Unit
            private set

        private var filterCount = 0
    }
}
