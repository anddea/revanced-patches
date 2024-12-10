package app.revanced.patches.youtube.feed.flyoutmenu

import app.revanced.patches.youtube.utils.resourceid.posterArtWidthDefault
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Compatible with YouTube v19.11.43~
 */
internal val bottomSheetMenuItemBuilderFingerprint = legacyFingerprint(
    name = "bottomSheetMenuItemBuilderFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "L",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT
    ),
    strings = listOf("Text missing for BottomSheetMenuItem with iconType: ")
)

/**
 * Compatible with ~YouTube v19.10.39
 */
internal val bottomSheetMenuItemBuilderLegacyFingerprint = legacyFingerprint(
    name = "bottomSheetMenuItemBuilderLegacyFingerprint",
    returnType = "L",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT
    ),
    strings = listOf("ElementTransformer, ElementPresenter and InteractionLogger cannot be null")
)

internal val contextualMenuItemBuilderFingerprint = legacyFingerprint(
    name = "contextualMenuItemBuilderFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.SYNTHETIC,
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.ADD_INT_2ADDR
    ),
    literals = listOf(posterArtWidthDefault),
)

