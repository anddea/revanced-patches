/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.music.general.components

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

/**
 * Renders the search suggestions response of the search page. Before a query is typed, the
 * response holds the search history shelves, the entry point buttons and the 'You may also like'
 * shelf. Matches the section list being passed to the list controller, right before the loading
 * spinner is updated.
 */
internal object SearchSuggestionsRenderFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L"),
    strings = listOf(
        "renderSuggestions",
        "Search Suggestions Response has Section List but also more than one renderer."
    ),
    filters = listOf(
        opcode(Opcode.NEW_INSTANCE),
        methodCall(
            opcode = Opcode.INVOKE_DIRECT,
            name = "<init>",
            parameters = listOf("L"),
            location = MatchAfterImmediately()
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            returnType = "V",
            parameters = listOf("L"),
            location = MatchAfterImmediately()
        ),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Lcom/google/android/apps/youtube/music/ui/components/loading/LoadingFrameLayout;",
            location = MatchAfterImmediately()
        )
    )
)

/**
 * A bit of the music shelf item bit field set when the item is a search history entry.
 */
private const val MUSIC_SHELF_ITEM_SEARCH_HISTORY_BIT = 0x8000000L

/**
 * A bit of the music shelf item bit field set when the item is a suggested search.
 */
private const val MUSIC_SHELF_ITEM_SUGGESTED_SEARCH_BIT = 0x4000000L

/**
 * Returns the renderers of a music shelf. Each shelf item holds one renderer,
 * selected by a bit of the item bit field.
 */
internal object MusicShelfItemsFingerprint : Fingerprint(
    returnType = "Ljava/util/List;",
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT),
        literal(MUSIC_SHELF_ITEM_SEARCH_HISTORY_BIT),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterWithin(6)),
        literal(MUSIC_SHELF_ITEM_SUGGESTED_SEARCH_BIT),
        fieldAccess(opcode = Opcode.IGET_OBJECT, location = MatchAfterWithin(6))
    )
)
