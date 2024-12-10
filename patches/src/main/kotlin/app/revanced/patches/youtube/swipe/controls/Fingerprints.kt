package app.revanced.patches.youtube.swipe.controls

import app.revanced.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.revanced.patches.youtube.utils.resourceid.fullScreenEngagementOverlay
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val fullScreenEngagementOverlayFingerprint = legacyFingerprint(
    name = "fullScreenEngagementOverlayFingerprint",
    returnType = "V",
    literals = listOf(fullScreenEngagementOverlay),
)

internal val hdrBrightnessFingerprint = legacyFingerprint(
    name = "hdrBrightnessFingerprint",
    returnType = "V",
    strings = listOf("mediaViewambientBrightnessSensor")
)

internal val swipeControlsHostActivityFingerprint = legacyFingerprint(
    name = "swipeControlsHostActivityFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = emptyList(),
    customFingerprint = { _, classDef ->
        classDef.type == "$EXTENSION_PATH/swipecontrols/SwipeControlsHostActivity;"
    }
)

/**
 * This fingerprint is compatible with YouTube v19.19.39+
 */
internal val swipeToSwitchVideoFingerprint = legacyFingerprint(
    name = "swipeToSwitchVideoFingerprint",
    returnType = "V",
    literals = listOf(45631116L),
)

/**
 * This fingerprint is compatible with YouTube v18.29.38+
 */
internal val watchPanelGesturesFingerprint = legacyFingerprint(
    name = "watchPanelGesturesFingerprint",
    returnType = "V",
    literals = listOf(45372793L),
)