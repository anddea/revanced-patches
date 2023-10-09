package app.revanced.patches.reddit.utils.settings.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object AcknowledgementsLabelBuilderFingerprint : MethodFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroidx/preference/Preference;"),
    strings = listOf("resources.getString(R.st\u2026g.label_acknowledgements)")
)