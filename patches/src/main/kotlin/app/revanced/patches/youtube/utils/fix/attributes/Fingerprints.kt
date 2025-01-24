package app.revanced.patches.youtube.utils.fix.attributes

import app.revanced.patches.youtube.utils.resourceid.ytOutlineMoonZ
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Tested on YouTube 19.25.xx ~ YouTube 20.02.xx.
 */
internal val setSleepTimerDrawableFingerprint = legacyFingerprint(
    name = "setSleepTimerDrawableFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z", "Ljava/lang/String;"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,      // Context.getResources()
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST,               // R.drawable.yt_outline_moon_z_vd_theme_24
        Opcode.INVOKE_VIRTUAL,      // Resources.getDrawable(int)
    ),
    literals = listOf(ytOutlineMoonZ),
)
