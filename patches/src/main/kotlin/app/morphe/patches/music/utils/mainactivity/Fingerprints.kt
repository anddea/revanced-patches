package app.morphe.patches.music.utils.mainactivity

import app.morphe.util.containsStringInstruction
import app.morphe.util.fingerprint.legacyFingerprint

// modified by lavinhoque33, 2026-10-04
// YouTube Music 9.40 moved both strings out of MusicActivity.onCreate,
// so the launcher activity class itself is accepted as well.
private const val MUSIC_ACTIVITY_CLASS_DESCRIPTOR =
    "Lcom/google/android/apps/youtube/music/activities/MusicActivity;"

internal val mainActivityFingerprint = legacyFingerprint(
    name = "mainActivityFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        method.name == "onCreate" && classDef.endsWith("Activity;") && (
                classDef.type == MUSIC_ACTIVITY_CLASS_DESCRIPTOR || (
                        method.containsStringInstruction("android.intent.action.MAIN") &&
                                method.containsStringInstruction("FEmusic_home")
                        )
                )
    }
)
