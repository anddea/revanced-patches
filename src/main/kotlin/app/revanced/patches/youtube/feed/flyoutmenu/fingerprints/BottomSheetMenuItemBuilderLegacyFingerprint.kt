package app.revanced.patches.youtube.feed.flyoutmenu.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

/**
 * Compatible with ~YouTube v19.10.39
 */
internal object BottomSheetMenuItemBuilderLegacyFingerprint : MethodFingerprint(
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