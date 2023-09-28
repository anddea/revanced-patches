package app.revanced.patches.youtube.flyoutpanel.player.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.BottomSheetFooterText
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.SubtitleMenuSettingsFooterInfo
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.AccessFlags

object CaptionsBottomSheetFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, _ ->
        methodDef.isWideLiteralExists(BottomSheetFooterText)
                && methodDef.isWideLiteralExists(SubtitleMenuSettingsFooterInfo)
    }
)