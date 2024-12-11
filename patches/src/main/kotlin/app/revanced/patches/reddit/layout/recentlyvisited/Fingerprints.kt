package app.revanced.patches.reddit.layout.recentlyvisited

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val communityDrawerPresenterFingerprint = legacyFingerprint(
    name = "communityDrawerPresenterFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(Opcode.AGET),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/CommunityDrawerPresenter;")
    }
)
