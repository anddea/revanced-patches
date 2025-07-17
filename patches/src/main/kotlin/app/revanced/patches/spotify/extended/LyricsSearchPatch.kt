package app.revanced.patches.spotify.extended

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.revanced.patcher.fingerprint
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val TRACK_LOGGER_CLASS_DESCRIPTOR = "Lapp/revanced/extension/spotify/misc/LyricsSearchManager;"
private const val MEDIA_SESSION_METADATA_TO_STRING_START = "MediaSessionTrackMetadata(uri="

val mediaSessionMetadataConstructorFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    returns("V")

    custom { _, classDef ->
        val toStringMethod = classDef.methods.firstOrNull {
            it.name == "toString" && it.parameters.isEmpty() && it.returnType == "Ljava/lang/String;"
        } ?: return@custom false

        val toStringInstructions = toStringMethod.instructionsOrNull ?: return@custom false
        toStringInstructions.any { instruction ->
            instruction.opcode == Opcode.CONST_STRING &&
                    (instruction as? ReferenceInstruction)?.reference?.let { ref ->
                        (ref as? StringReference)?.string?.startsWith(MEDIA_SESSION_METADATA_TO_STRING_START) == true
                    } == true
        }
    }
}

@Suppress("unused")
val lyricsSearchPatch = bytecodePatch(
    name = "Lyrics search",
    description = "Displays a \"Search Lyrics\" panel in the Main Activity that searches for lyrics on Google, and song meanings on Songtell. The activity is set to SpotifyMainActivity, so the \"Search Lyrics\" panel won't be shown in NowPlayingActivity (Player view) or possibly other activities.",
) {
    compatibleWith("com.spotify.music")
    dependsOn(setActivityContextPatch)

    execute {
        val targetConstructor = mediaSessionMetadataConstructorFingerprint.method
        val returnVoidIndex = targetConstructor.indexOfFirstInstructionOrThrow(Opcode.RETURN_VOID)

        val smaliCodeToInsert = """
            invoke-static {p2, p6}, $TRACK_LOGGER_CLASS_DESCRIPTOR->processTrackMetadata(Ljava/lang/Object;Ljava/lang/Object;)V
        """.trimIndent()

        targetConstructor.addInstructions(returnVoidIndex, smaliCodeToInsert)
    }
}
