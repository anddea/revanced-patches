package app.morphe.patches.music.ads.general

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter.Companion.opcodesToFilters
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.string
import app.morphe.patcher.methodCall
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import app.morphe.util.containsLiteralInstruction
import app.morphe.patches.music.utils.resourceid.buttonContainer
import app.morphe.patches.music.utils.resourceid.floatingLayout
import app.morphe.patches.music.utils.resourceid.modernDialogBackground
import app.morphe.patches.music.utils.resourceid.musicNotifierShelf
import app.morphe.patches.music.utils.resourceid.privacyTosFooter
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object accountMenuFooterFingerprint : Fingerprint(
    returnType = "L",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    filters = opcodesToFilters(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT
    ),
    custom = { method, _ -> method.containsLiteralInstruction(privacyTosFooter) }
)

internal object floatingLayoutFingerprint : Fingerprint(
    returnType = "Landroid/view/View;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    custom = { method, _ -> method.containsLiteralInstruction(floatingLayout) }
)

internal object getPremiumDialogParentFingerprint : Fingerprint(
    returnType = "Landroid/graphics/drawable/Drawable;",
    accessFlags = listOf(AccessFlags.PROTECTED),
    parameters = listOf("Landroid/content/Context;"),
    custom = { method, _ -> method.containsLiteralInstruction(modernDialogBackground) }
)

internal object getPremiumDialogFingerprint : Fingerprint(
    returnType = "Landroid/app/Dialog;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/os/Bundle;"),
    classFingerprint = getPremiumDialogParentFingerprint,
    custom = { method, _ ->
        indexOfSetContentViewInstruction(method) >= 0
    }
)

internal fun indexOfSetContentViewInstruction(method: Method) =
    method.indexOfFirstInstruction {
        getReference<MethodReference>()?.toString() == "Landroid/app/Dialog;->setContentView(Landroid/view/View;)V"
    }

internal object getPremiumTextViewFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    filters = opcodesToFilters(
        Opcode.IGET_BOOLEAN,
        Opcode.CONST_4,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC
    ),
    strings = listOf(string("FEmusic_history", StringComparisonType.CONTAINS))
)

// 9.40: the visibility constant after the "get premium" flag is `const/16` instead of `const/4`.
internal object getPremiumTextViewV940Fingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = emptyList(),
    filters = opcodesToFilters(
        Opcode.IGET_BOOLEAN,
        Opcode.CONST_16,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC
    ),
    strings = listOf(string("FEmusic_history", StringComparisonType.CONTAINS))
)

internal object notifierShelfFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    custom = { method, _ ->
        method.containsLiteralInstruction(musicNotifierShelf) && method.containsLiteralInstruction(buttonContainer)
    }
)

/**
 * Lazily builds the in-player ad overlay (ad title, ad progress text, skip ad button).
 * Matches the findViewById call for skip_ad_button, whose receiver is the overlay root.
 *
 * Older app versions build the overlay in a separate method that returns the root view,
 * newer versions inline it into a method with no parameters. The method signature is not
 * declared so both match.
 */
internal object AdOverlayInflateFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "default_ad_overlay"),
        methodCall(name = "inflate"),
        resourceLiteral(ResourceType.ID, "skip_ad_button"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            name = "findViewById",
            returnType = "Landroid/view/View;"
        )
    )
)
