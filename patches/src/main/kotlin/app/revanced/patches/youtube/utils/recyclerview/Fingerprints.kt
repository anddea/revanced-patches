package app.revanced.patches.youtube.utils.recyclerview

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal const val RECYCLER_VIEW_BUILDER_FEATURE_FLAG = 45382015L

internal val recyclerViewBuilderFingerprint = legacyFingerprint(
    name = "recyclerViewBuilderFingerprint",
    literals = listOf(RECYCLER_VIEW_BUILDER_FEATURE_FLAG),
)

internal val recyclerViewTreeObserverFingerprint = legacyFingerprint(
    name = "recyclerViewTreeObserverFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    strings = listOf("LithoRVSLCBinder"),
    customFingerprint = { method, _ ->
        val parameterTypes = method.parameterTypes
        parameterTypes.size > 2 &&
                parameterTypes[1] == "Landroid/support/v7/widget/RecyclerView;"
    }
)
