package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.BottomSheetMargins
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.AccessFlags

object NewFlyoutPanelBuilderFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    returnType = "Landroid/widget/LinearLayout;",
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(BottomSheetMargins) }
)