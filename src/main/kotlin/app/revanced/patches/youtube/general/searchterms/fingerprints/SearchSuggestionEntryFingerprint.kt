package app.revanced.patches.youtube.general.searchterms.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.SearchSuggestionEntry
import app.revanced.util.bytecode.isWideLiteralExists

object SearchSuggestionEntryFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(SearchSuggestionEntry) }
)