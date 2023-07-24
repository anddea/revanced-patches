package app.revanced.patches.reddit.layout.place.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object HomePagerScreenFingerprint : MethodFingerprint(
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/LayoutInflater;", "Landroid/view/ViewGroup;"),
    strings = listOf("view.findViewById(Search\u2026nav_search_cta_container)"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/HomePagerScreen;")
    }
)