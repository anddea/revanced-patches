package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.InlineTimeBarColorizedBarPlayedColorDark
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.InlineTimeBarPlayedNotHighlightedColor
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.AccessFlags

object PlayerSeekbarColorFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    customFingerprint = { methodDef, _ ->
        methodDef.isWideLiteralExists(InlineTimeBarColorizedBarPlayedColorDark)
                && methodDef.isWideLiteralExists(InlineTimeBarPlayedNotHighlightedColor)
    }
)