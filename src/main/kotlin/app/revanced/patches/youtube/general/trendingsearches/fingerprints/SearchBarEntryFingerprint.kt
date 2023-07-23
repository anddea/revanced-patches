package app.revanced.patches.youtube.general.trendingsearches.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.YtOutlineArrowTimeBlack
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.YtOutlineFireBlack
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.YtOutlineSearchBlack
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.AccessFlags

object SearchBarEntryFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, _ ->
        methodDef.isWideLiteralExists(YtOutlineArrowTimeBlack)
                && methodDef.isWideLiteralExists(YtOutlineFireBlack)
                && methodDef.isWideLiteralExists(YtOutlineSearchBlack)
    }
)