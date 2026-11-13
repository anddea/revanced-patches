/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.player.comments

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags

internal object ShortsLiveStreamEmojiPickerOnClickListenerFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("L"),
    filters = listOf(
        literal(126326492L),
    )
)

internal object ShortsLiveStreamEmojiPickerOpacityFingerprint : Fingerprint(
    returnType = "Landroid/widget/ImageView;",
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    parameters = emptyList(),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "emoji_picker_icon"),
    )
)

/**
 * Reads the payload of an entity sent by the server. Its field number is the type of the entity.
 */
internal object EntityMutationPayloadFingerprint : Fingerprint(
    returnType = "[B",
    strings = listOf("There can be only one field in EntityMutationPayload")
)
