package app.morphe.patches.youtube.utils.castbutton

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags

internal object MenuItemInitializeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "castmediaroutebutton"),
        methodCall(name = "setShowAsAction"),
    ),
)

internal object MenuItemVisibilityFingerprint : Fingerprint(
    classFingerprint = MenuItemInitializeFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("Z"),
    filters = listOf(methodCall(name = "setVisible")),
)

internal object ModernMenuItemVisibilityFingerprint : Fingerprint(
    classFingerprint = MenuItemInitializeFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(methodCall(name = "setVisible")),
)
