package app.morphe.patches.youtube.general.updates

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val cronetHeaderFingerprint = legacyFingerprint(
    name = "cronetHeaderFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
    strings = listOf("Accept-Encoding"),
    // In YouTube 19.16.39 or earlier, there are two methods with almost the same structure.
    // Check the fields of the class to identify them correctly.
    customFingerprint = { _, classDef ->
        classDef.fields.find { it.type == "J" } != null
    }
)
