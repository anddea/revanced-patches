package app.revanced.patches.music.general.components

import app.revanced.patches.music.utils.resourceid.chipCloud
import app.revanced.patches.music.utils.resourceid.historyMenuItem
import app.revanced.patches.music.utils.resourceid.musicTasteBuilderShelf
import app.revanced.patches.music.utils.resourceid.offlineSettingsMenuItem
import app.revanced.patches.music.utils.resourceid.playerOverlayChip
import app.revanced.patches.music.utils.resourceid.toolTipContentView
import app.revanced.patches.music.utils.resourceid.topBarMenuItemImageView
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val chipCloudFingerprint = legacyFingerprint(
    name = "chipCloudFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT
    ),
    literals = listOf(chipCloud),
)

internal val contentPillFingerprint = legacyFingerprint(
    name = "contentPillFingerprint",
    returnType = "V",
    strings = listOf("Content pill VE is null")
)

internal val floatingButtonFingerprint = legacyFingerprint(
    name = "floatingButtonFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(Opcode.AND_INT_LIT16)
)

internal val floatingButtonParentFingerprint = legacyFingerprint(
    name = "floatingButtonParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(Opcode.INVOKE_DIRECT),
    literals = listOf(259982244L),
)

internal val historyMenuItemFingerprint = legacyFingerprint(
    name = "historyMenuItemFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/Menu;"),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.RETURN_VOID
    ),
    literals = listOf(historyMenuItem),
    customFingerprint = { _, classDef ->
        classDef.methods.count() == 5
    }
)

internal val historyMenuItemOfflineTabFingerprint = legacyFingerprint(
    name = "historyMenuItemOfflineTabFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/Menu;"),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.RETURN_VOID
    ),
    literals = listOf(historyMenuItem, offlineSettingsMenuItem),
)

internal val mediaRouteButtonFingerprint = legacyFingerprint(
    name = "mediaRouteButtonFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    strings = listOf("MediaRouteButton")
)

internal val parentToolMenuFingerprint = legacyFingerprint(
    name = "parentToolMenuFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET,
    ),
    strings = listOf("pref_key_parent_tools"),
    customFingerprint = { method, _ ->
        method.name == "onSettingsLoaded"
    }
)

internal val playerOverlayChipFingerprint = legacyFingerprint(
    name = "playerOverlayChipFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(playerOverlayChip),
)

internal val preferenceScreenFingerprint = legacyFingerprint(
    name = "preferenceScreenFingerprint",
    returnType = "V",
    customFingerprint = { method, _ ->
        method.definingClass == "Lcom/google/android/apps/youtube/music/settings/fragment/SettingsHeadersFragment;" &&
                method.name == "onCreatePreferences"
    }
)

internal val searchBarFingerprint = legacyFingerprint(
    name = "searchBarFingerprint",
    returnType = "V",
    customFingerprint = { method, _ ->
        indexOfVisibilityInstruction(method) >= 0
    }
)

fun indexOfVisibilityInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "setVisibility"
    }

internal val searchBarParentFingerprint = legacyFingerprint(
    name = "searchBarParentFingerprint",
    returnType = "Landroid/content/Intent;",
    strings = listOf("web_search")
)

internal val soundSearchFingerprint = legacyFingerprint(
    name = "soundSearchFingerprint",
    parameters = emptyList(),
    literals = listOf(45625491L),
)

internal val tasteBuilderConstructorFingerprint = legacyFingerprint(
    name = "tasteBuilderConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(musicTasteBuilderShelf),
)

internal val tasteBuilderSyntheticFingerprint = legacyFingerprint(
    name = "tasteBuilderSyntheticFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.SYNTHETIC,
    parameters = listOf("L", "Ljava/lang/Object;"),
    opcodes = listOf(
        Opcode.IF_NEZ,
        Opcode.IGET_OBJECT
    )
)

internal val tooltipContentViewFingerprint = legacyFingerprint(
    name = "tooltipContentViewFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    literals = listOf(toolTipContentView),
)

internal val topBarMenuItemImageViewFingerprint = legacyFingerprint(
    name = "topBarMenuItemImageViewFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(topBarMenuItemImageView),
)

