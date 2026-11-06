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
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Tabs of the browse response (singleColumnBrowseResultsRenderer).
 * The tab protos are converted to tab items, that are cached in a field of the class.
 */
internal object BrowseResponseTabsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL, AccessFlags.DECLARED_SYNCHRONIZED),
    returnType = "L",
    parameters = listOf(),
    filters = listOf(
        literal(58173949L),
        methodCall(name = "stream"),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this"
        )
    ),
    custom = { _, classDef ->
        classDef.interfaces.contains("Landroid/os/Parcelable;")
    }
)

/**
 * Creates a navigation bar item proto with a browse endpoint.
 * Parameters: browse id, label, icon type, icon only.
 */
internal object PivotBarItemProtoFactoryFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "L", "Z"),
    filters = listOf(
        literal(117501096L)
    )
)

/**
 * Navigation bar used before the guide response is loaded.
 * The Subscriptions button is not added if the flag is enabled.
 */
internal object OfflineNavigationBarSubscriptionsFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45780222L),
        string("FEsubscriptions")
    )
)

