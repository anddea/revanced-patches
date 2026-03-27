package app.morphe.patches.music.misc.share

import app.morphe.patches.music.utils.resourceid.bottomSheetRecyclerView
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val bottomSheetRecyclerViewFingerprint = legacyFingerprint(
    name = "bottomSheetRecyclerViewFingerprint",
    returnType = "Lj${'$'}/util/Optional;",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(bottomSheetRecyclerView),
)
