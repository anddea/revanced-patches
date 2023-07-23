package app.revanced.patches.youtube.shorts.commentpopuppanels.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists
import com.android.tools.smali.dexlib2.AccessFlags

object ReelWatchFragmentBuilderFingerprint : MethodFingerprint(
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(
        "Landroid/view/LayoutInflater;",
        "Landroid/view/ViewGroup;",
        "Landroid/os/Bundle;"
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWide32LiteralExists(45401415) }
)