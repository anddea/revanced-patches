package app.revanced.patches.music.utils.litho.patch

import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.patches.shared.fingerprints.IdentifierFingerprint
import app.revanced.patches.shared.patch.litho.ComponentParserPatch
import app.revanced.patches.shared.patch.litho.ComponentParserPatch.Companion.identifierHook
import app.revanced.util.integrations.Constants.MUSIC_ADS_PATH

@DependsOn([ComponentParserPatch::class])
@YouTubeMusicCompatibility
@Version("0.0.1")
class MusicLithoFilterPatch : BytecodePatch(
    listOf(IdentifierFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        identifierHook("$MUSIC_ADS_PATH/MusicLithoFilterPatch;->filter")

        return PatchResultSuccess()
    }
}
