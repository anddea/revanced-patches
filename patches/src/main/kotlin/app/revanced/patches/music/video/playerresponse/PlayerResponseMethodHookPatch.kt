package app.revanced.patches.music.video.playerresponse

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.utils.playservice.is_7_03_or_greater
import app.revanced.patches.music.utils.playservice.versionCheckPatch
import app.revanced.util.fingerprint.methodOrThrow

private const val REGISTER_VIDEO_ID = "p1"
private const val REGISTER_PLAYLIST_ID = "p4"
private const val REGISTER_PLAYLIST_INDEX = "p5"

private lateinit var playerResponseMethod: MutableMethod

val playerResponseMethodHookPatch = bytecodePatch(
    description = "playerResponseMethodHookPatch"
) {
    dependsOn(versionCheckPatch)

    execute {
        playerResponseMethod = if (is_7_03_or_greater) {
            playerParameterBuilderFingerprint
        } else {
            playerParameterBuilderLegacyFingerprint
        }.methodOrThrow()
    }
}

fun hookPlayerResponse(
    descriptor: String,
    onlyVideoId: Boolean = false
) {
    val smaliInstruction = if (onlyVideoId)
        "invoke-static {$REGISTER_VIDEO_ID}, $descriptor"
    else
        "invoke-static {$REGISTER_VIDEO_ID, $REGISTER_PLAYLIST_ID, $REGISTER_PLAYLIST_INDEX}, $descriptor"

    playerResponseMethod.addInstruction(0, smaliInstruction)
}
