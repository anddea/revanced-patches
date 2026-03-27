package app.morphe.patches.youtube.utils.gms

import app.morphe.patches.youtube.utils.resourceid.icOfflineNoContentUpsideDown
import app.morphe.patches.youtube.utils.resourceid.offlineNoContentBodyTextNotOfflineEligible
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val specificNetworkErrorViewControllerFingerprint = legacyFingerprint(
    name = "specificNetworkErrorViewControllerFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = emptyList(),
    literals = listOf(icOfflineNoContentUpsideDown, offlineNoContentBodyTextNotOfflineEligible),
)

// It's not clear if this second class is ever used and it may be dead code,
// but it the layout image / text is identical to the network error fingerprint above.
internal val loadingFrameLayoutControllerFingerprint = legacyFingerprint(
    name = "loadingFrameLayoutControllerFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("L"),
    literals = listOf(icOfflineNoContentUpsideDown, offlineNoContentBodyTextNotOfflineEligible),
)
