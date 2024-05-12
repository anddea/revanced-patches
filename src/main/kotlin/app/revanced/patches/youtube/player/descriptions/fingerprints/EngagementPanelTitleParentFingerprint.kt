package app.revanced.patches.youtube.player.descriptions.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object EngagementPanelTitleParentFingerprint : MethodFingerprint(
    strings = listOf("[EngagementPanelTitleHeader] Cannot remove action buttons from header as the child count is out of sync. Buttons to remove exceed current header child count.")
)