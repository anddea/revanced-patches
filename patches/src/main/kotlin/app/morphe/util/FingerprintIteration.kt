/*
 * Copyright 2025 Morphe.
 * https://github.com/MorpheApp/morphe-patches-library
 *
 * Original code hard forked from:
 * https://github.com/ReVanced/revanced-patches/blob/724e6d61b2ecd868c1a9a37d465a688e83a74799/patches/src/main/kotlin/app/revanced/util/BytecodeUtils.kt
 *
 * File-Specific License Notice (GPLv3 Section 7 Terms)
 *
 * This file is part of the Morphe project and is licensed under
 * the GNU General Public License version 3 (GPLv3), with the Additional
 * Terms under Section 7 described in the LICENSE file.
 *
 * https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Section 7b: Notice Preservation
 * -------------------------------
 * This entire comment block must be preserved in all copies,
 * distributions, and derivative works of this file, in both
 * original and modified source forms.
 *
 * Portions of this software are provided "AS IS" by the Morphe software project.
 * Any express or implied warranties, including the implied warranties of
 * merchantability and fitness for a particular purpose, are disclaimed.
 */

package app.morphe.util

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod

// TODO If this function remains unchanged for a while then move this to morphe-patcher.
/**
 * Iterate across all method indexes that match an [Fingerprint].
 * At this time, only a single [InstructionFilter] is supported.
 *
 * This differs from using [matchAll] as this matches multiple instruction
 * indexes in the same method and [matchAll] matches only the first index
 * of each method.
 *
 * @param requireMatches If true and no matches exist, an exception is thrown.
 * @param block Method iteration block. Indexes are iterated from last to first.
 */
context(patchContext: BytecodePatchContext)
fun Fingerprint.matchAllMethodIndicesForEach(
    requireMatches: Boolean = true,
    block: MutableMethod.(Int) -> Unit
) {
    requireNotNull(filters)
    require(filters!!.size == 1) {
        "Fingerprint must contain exactly 1 filter"
    }

    val matches = matchAllOrNull()
    if (matches == null) {
        if (requireMatches) throw PatchException("Could not find any matches of $this")
        return
    }

    val filter = filters!!.first()
    matches.forEach { match ->
        val method = match.method
        method.findInstructionIndicesReversedOrThrow(filter).forEach { index ->
            block(method, index)
        }
    }
}
