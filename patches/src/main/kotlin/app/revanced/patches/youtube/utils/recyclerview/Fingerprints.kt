package app.revanced.patches.youtube.utils.recyclerview

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val bottomSheetRecyclerViewBuilderFingerprint = legacyFingerprint(
    name = "bottomSheetRecyclerViewBuilderFingerprint",
    literals = listOf(45382015L),
)

internal val recyclerViewTreeObserverFingerprint = legacyFingerprint(
    name = "recyclerViewTreeObserverFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    strings = listOf("LithoRVSLCBinder")
)
