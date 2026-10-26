/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.navigation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

/**
 * Sets the bottom margin of the layout that holds the page and the navigation bar.
 * With the keyboard open and a text field focused, the margin is the keyboard height,
 * otherwise the bottom of the window insets.
 */
internal object PageBottomMarginFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/graphics/Rect;"),
    filters = listOf(
        methodCall(smali = "Landroid/view/View;->findFocus()Landroid/view/View;"),
        methodCall(
            smali = "Landroid/view/View;->onCheckIsTextEditor()Z",
            location = MatchAfterWithin(5)
        ),
        fieldAccess(
            opcode = Opcode.IGET,
            definingClass = "Landroid/graphics/Rect;",
            name = "bottom"
        )
    )
)

/**
 * Sets the padding of a feed list. When the feed is laid out behind the navigation bar,
 * the bottom padding holds the navigation bar height and the bottom window inset.
 */
internal object FeedListPaddingFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/support/v7/widget/RecyclerView;",
            name = "setClipToPadding"
        ),
        methodCall(
            definingClass = "Landroid/support/v7/widget/RecyclerView;",
            name = "setPadding",
            location = MatchAfterImmediately()
        )
    )
)

