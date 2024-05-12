package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.player.components.PlayerComponentsPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.FadeDurationFast
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.InsetOverlayViewLayout
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ScrimOverlay
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.SeekUndoEduOverlayStub
import app.revanced.patches.youtube.utils.sponsorblock.SponsorBlockBytecodePatch
import app.revanced.util.containsWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Several instructions are added to this method by different patches.
 * Therefore, patches using this fingerprint should not use the [Opcode] pattern,
 * and must access the index through the resourceId.
 *
 * The patches and resourceIds that use this fingerprint are as follows:
 * - [PlayerComponentsPatch] uses [FadeDurationFast], [ScrimOverlay] and [SeekUndoEduOverlayStub].
 * - [SponsorBlockBytecodePatch] uses [InsetOverlayViewLayout].
 */
internal object YouTubeControlsOverlayFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = emptyList(),
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionIndex(FadeDurationFast)
                && methodDef.containsWideLiteralInstructionIndex(InsetOverlayViewLayout)
                && methodDef.containsWideLiteralInstructionIndex(ScrimOverlay)
                && methodDef.containsWideLiteralInstructionIndex(SeekUndoEduOverlayStub)
    }
)