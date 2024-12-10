package app.revanced.patches.youtube.general.toolbar

import app.revanced.patches.youtube.utils.resourceid.actionBarRingo
import app.revanced.patches.youtube.utils.resourceid.actionBarRingoBackground
import app.revanced.patches.youtube.utils.resourceid.drawerContentView
import app.revanced.patches.youtube.utils.resourceid.voiceSearch
import app.revanced.patches.youtube.utils.resourceid.youTubeLogo
import app.revanced.patches.youtube.utils.resourceid.ytOutlineVideoCamera
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionReversed
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

internal val actionBarRingoBackgroundFingerprint = legacyFingerprint(
    name = "actionBarRingoBackgroundFingerprint",
    returnType = "Landroid/view/View;",
    literals = listOf(actionBarRingoBackground),
    customFingerprint = { method, _ ->
        indexOfStaticInstruction(method) >= 0
    }
)

internal fun indexOfStaticInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_STATIC &&
                reference?.parameterTypes?.size == 1 &&
                reference.parameterTypes.firstOrNull() == "Landroid/content/Context;" &&
                reference.returnType == "Z"
    }

internal val actionBarRingoConstructorFingerprint = legacyFingerprint(
    name = "actionBarRingoConstructorFingerprint",
    returnType = "V",
    strings = listOf("default"),
    customFingerprint = custom@{ method, _ ->
        if (!MethodUtil.isConstructor(method)) {
            return@custom false
        }

        val parameterTypes = method.parameterTypes
        parameterTypes.size >= 5 && parameterTypes[0] == "Landroid/content/Context;"
    }
)

internal val actionBarRingoTextFingerprint = legacyFingerprint(
    name = "actionBarRingoTextFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { method, _ ->
        indexOfStartDelayInstruction(method) >= 0 &&
                indexOfStaticInstructions(method) >= 0
    }
)

internal fun indexOfStartDelayInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "setStartDelay"
    }

internal fun indexOfStaticInstructions(method: Method) =
    method.indexOfFirstInstructionReversed(indexOfStartDelayInstruction(method)) {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_STATIC &&
                reference?.parameterTypes?.size == 1 &&
                reference.parameterTypes.firstOrNull() == "Landroid/content/Context;" &&
                reference.returnType == "Z"
    }

internal val attributeResolverFingerprint = legacyFingerprint(
    name = "attributeResolverFingerprint",
    returnType = "Landroid/graphics/drawable/Drawable;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("Landroid/content/Context;", "I"),
    strings = listOf("Type of attribute is not a reference to a drawable (attr = %d, value = %s)")
)

internal val createButtonDrawableFingerprint = legacyFingerprint(
    name = "createButtonDrawableFingerprint",
    literals = listOf(ytOutlineVideoCamera),
)

internal val createSearchSuggestionsFingerprint = legacyFingerprint(
    name = "createSearchSuggestionsFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I", "Landroid/view/View;", "Landroid/view/ViewGroup;"),
    strings = listOf("ss_rds")
)

internal val drawerContentViewConstructorFingerprint = legacyFingerprint(
    name = "drawerContentViewConstructorFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(drawerContentView),
)

internal val drawerContentViewFingerprint = legacyFingerprint(
    name = "drawerContentViewFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
    ),
    customFingerprint = { method, _ ->
        indexOfAddViewInstruction(method) >= 0
    }
)

internal fun indexOfAddViewInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "addView"
    }

/**
 * This fingerprint is compatible with YouTube v19.07.40+
 */
internal val imageSearchButtonConfigFingerprint = legacyFingerprint(
    name = "imageSearchButtonConfigFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(45617544L),
)

internal val searchBarFingerprint = legacyFingerprint(
    name = "searchBarFingerprint",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ
    ),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstructionReversed {
            getReference<MethodReference>()?.name == "isEmpty"
        } >= 0
    }
)

internal val searchBarParentFingerprint = legacyFingerprint(
    name = "searchBarParentFingerprint",
    returnType = "Landroid/view/View;",
    strings = listOf("voz-target-id"),
    literals = listOf(voiceSearch),
)

internal val searchResultFingerprint = legacyFingerprint(
    name = "searchResultFingerprint",
    returnType = "Landroid/view/View;",
    strings = listOf("search_filter_chip_applied", "search_original_chip_query"),
    literals = listOf(voiceSearch),
)

internal val setActionBarRingoFingerprint = legacyFingerprint(
    name = "setActionBarRingoFingerprint",
    returnType = "L",
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_STATIC
    ),
    literals = listOf(actionBarRingo),
)

internal val setWordMarkHeaderFingerprint = legacyFingerprint(
    name = "setWordMarkHeaderFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("Landroid/widget/ImageView;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.CONST,
        Opcode.INVOKE_STATIC,
    )
)

@Suppress("SpellCheckingInspection")
internal val yoodlesImageViewFingerprint = legacyFingerprint(
    name = "yoodlesImageViewFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L"),
    returnType = "Landroid/view/View;",
    literals = listOf(youTubeLogo)
)

internal val youActionBarFingerprint = legacyFingerprint(
    name = "youActionBarFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
    )
)