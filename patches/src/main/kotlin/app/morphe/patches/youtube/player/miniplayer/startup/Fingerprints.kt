@file:Suppress("SpellCheckingInspection")

package app.morphe.patches.youtube.player.miniplayer.startup

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Tested on YouTube 18.25.40 ~ 20.05.44
 *
 * This fingerprint is not compatible with YouTube 18.19.36 or earlier
 */
internal val showMiniplayerCommandFingerprint = legacyFingerprint(
    name = "showMiniplayerCommandFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Ljava/util/Map;"),
    // Opcode pattern looks very weak, but it's not really.
    opcodes = listOf(
        Opcode.IF_NEZ,
        Opcode.IF_EQZ,
    ),
    literals = listOf(121253L, 164817L),
)