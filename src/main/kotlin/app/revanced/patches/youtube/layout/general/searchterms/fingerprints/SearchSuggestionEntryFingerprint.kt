package app.revanced.patches.youtube.layout.general.searchterms.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.searchSuggestionEntryId
import app.revanced.util.bytecode.isWideLiteralExists

object SearchSuggestionEntryFingerprint : MethodFingerprint(
    customFingerprint = { it, _ -> it.isWideLiteralExists(searchSuggestionEntryId) }
)