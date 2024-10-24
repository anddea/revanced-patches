package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MiniPlayerViewPager
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.PlayerViewPager
import app.revanced.util.containsWideLiteralInstructionValue
import com.android.tools.smali.dexlib2.AccessFlags

internal object PlayerViewPagerConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionValue(MiniPlayerViewPager)
                && methodDef.containsWideLiteralInstructionValue(PlayerViewPager)
    },
)