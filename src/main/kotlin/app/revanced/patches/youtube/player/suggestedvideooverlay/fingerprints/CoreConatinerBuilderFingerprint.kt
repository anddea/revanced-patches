package app.revanced.patches.youtube.player.suggestedvideooverlay.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.CoreContainer
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.AccessFlags

object CoreConatinerBuilderFingerprint : MethodFingerprint(
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/content/Context;"),
    customFingerprint = { methodDef, _ ->
        methodDef.isWideLiteralExists(
            CoreContainer
        )
    })