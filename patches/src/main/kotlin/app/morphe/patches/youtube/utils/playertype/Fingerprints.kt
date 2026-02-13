package app.morphe.patches.youtube.utils.playertype

import app.morphe.patcher.Fingerprint
import app.morphe.patches.youtube.utils.resourceid.reelWatchPlayer
import app.morphe.patches.youtube.utils.resourceid.toolbarContainerId
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

internal object PlayerTypeEnumFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    strings = listOf(
        "NONE",
        "HIDDEN",
        "WATCH_WHILE_MINIMIZED",
        "WATCH_WHILE_MAXIMIZED",
        "WATCH_WHILE_FULLSCREEN",
        "WATCH_WHILE_SLIDING_MAXIMIZED_FULLSCREEN",
        "WATCH_WHILE_SLIDING_MINIMIZED_MAXIMIZED",
        "WATCH_WHILE_SLIDING_MINIMIZED_DISMISSED",
        "INLINE_MINIMAL",
        "VIRTUAL_REALITY_FULLSCREEN",
        "WATCH_WHILE_PICTURE_IN_PICTURE",
    )
)

internal val adProgressTextViewVisibilityFingerprint = legacyFingerprint(
    name = "adProgressTextViewVisibilityFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z"),
    customFingerprint = { method, _ ->
        indexOfAdProgressTextViewVisibilityInstruction(method) >= 0
    }
)

internal fun indexOfAdProgressTextViewVisibilityInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.toString() ==
                "Lcom/google/android/libraries/youtube/ads/player/ui/AdProgressTextView;->setVisibility(I)V"
    }

internal val browseIdClassFingerprint = legacyFingerprint(
    name = "browseIdClassFingerprint",
    returnType = "Ljava/lang/Object;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.SYNTHETIC,
    parameters = listOf("Ljava/lang/Object;", "L"),
    strings = listOf("VL")
)

internal val componentHostFingerprint = legacyFingerprint(
    name = "componentHostFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    customFingerprint = { method, classDef ->
        classDef.superclass == "Lcom/facebook/litho/ComponentHost;" &&
                indexOfGetContextInstruction(method) >= 0
    }
)

internal fun indexOfGetContextInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.IGET_OBJECT &&
                getReference<FieldReference>()?.type == "Landroid/content/Context;"
    }

internal val playerTypeFingerprint = legacyFingerprint(
    name = "playerTypeFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IF_NE,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/YouTubePlayerOverlaysLayout;")
    }
)

internal val reelWatchPagerFingerprint = legacyFingerprint(
    name = "reelWatchPagerFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(reelWatchPlayer),
)

internal fun indexOfStringIsEmptyInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>().toString() == "Ljava/lang/String;->isEmpty()Z"
    }

internal val searchQueryClassFingerprint = legacyFingerprint(
    name = "searchQueryClassFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Ljava/util/Map;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
    ),
    strings = listOf("force_enable_sticky_browsy_bars"),
    customFingerprint = { method, _ ->
        indexOfStringIsEmptyInstruction(method) >= 0
    }
)

internal val toolbarLayoutFingerprint = legacyFingerprint(
    name = "toolbarLayoutFingerprint",
    literals = listOf(toolbarContainerId),
    customFingerprint = { method, _ ->
        method.name == "<init>" &&
                indexOfMainCollapsingToolbarLayoutInstruction(method) >= 0
    }
)

internal fun indexOfMainCollapsingToolbarLayoutInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.CHECK_CAST &&
                getReference<TypeReference>()?.type == "Lcom/google/android/apps/youtube/app/ui/actionbar/MainCollapsingToolbarLayout;"
    }

/**
 * Matches to https://android.googlesource.com/platform/frameworks/support/+/9eee6ba/v7/appcompat/src/android/support/v7/widget/Toolbar.java#963
 */
internal val appCompatToolbarBackButtonFingerprint = legacyFingerprint(
    name = "appCompatToolbarBackButtonFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Landroid/graphics/drawable/Drawable;",
    parameters = emptyList(),
    customFingerprint = { _, classDef ->
        classDef.type == "Landroid/support/v7/widget/Toolbar;"
    },
)

internal val videoStateFingerprint = legacyFingerprint(
    name = "videoStateFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Lcom/google/android/libraries/youtube/player/features/overlay/controls/ControlsState;"),
    opcodes = listOf(
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT, // obfuscated parameter field name
        Opcode.IGET_OBJECT,
        Opcode.IF_NE,
    ),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "equals"
        } >= 0
    },
)
