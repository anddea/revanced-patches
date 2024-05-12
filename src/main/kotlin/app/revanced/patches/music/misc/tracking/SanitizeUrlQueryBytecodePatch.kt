package app.revanced.patches.music.misc.tracking

import app.revanced.patches.music.misc.tracking.fingerprints.ShareLinkFormatterFingerprint
import app.revanced.patches.music.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.shared.tracking.BaseSanitizeUrlQueryPatch
import app.revanced.patches.shared.tracking.fingerprints.CopyTextEndpointFingerprint

object SanitizeUrlQueryBytecodePatch : BaseSanitizeUrlQueryPatch(
    "$MISC_PATH/SanitizeUrlQueryPatch;",
    listOf(
        CopyTextEndpointFingerprint,
        ShareLinkFormatterFingerprint
    ),
    null
)
