package app.morphe.patches.youtube.general.toolbar

import app.morphe.patches.youtube.utils.resourceid.actionBarRingo
import app.morphe.patches.youtube.utils.resourceid.actionBarRingoBackground
import app.morphe.patches.youtube.utils.resourceid.drawerContentView
import app.morphe.patches.youtube.utils.resourceid.menuSearch
import app.morphe.patches.youtube.utils.resourceid.p13nHeader
import app.morphe.patches.youtube.utils.resourceid.seeMoreProceedingHeader
import app.morphe.patches.youtube.utils.resourceid.voiceSearch
import app.morphe.patches.youtube.utils.resourceid.youTubeLogo
import app.morphe.patches.youtube.utils.resourceid.ytOutlineVideoCamera
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.or
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
        indexOfActionBarRingoBackgroundTabletInstruction(method) >= 0
    }
)

internal fun indexOfActionBarRingoBackgroundTabletInstruction(method: Method) =
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
                indexOfActionBarRingoTextTabletInstructions(method) >= 0
    }
)

internal fun indexOfStartDelayInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "setStartDelay"
    }

internal fun indexOfActionBarRingoTextTabletInstructions(method: Method) =
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

/**
 * Matches using the class found in [searchSuggestionCollectionFingerprint].
 */
internal val createSearchSuggestionsFingerprint = legacyFingerprint(
    name = "createSearchSuggestionsFingerprint",
    returnType = "Landroid/view/View;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I", "Landroid/view/View;", "Landroid/view/ViewGroup;"),
    strings = listOf("ss_rds"),
    customFingerprint = { method, _ ->
        indexOfIteratorInstruction(method) >= 0
    }
)

internal fun indexOfIteratorInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_INTERFACE &&
                getReference<MethodReference>()?.toString() == "Ljava/util/Iterator;->next()Ljava/lang/Object;"
    }

// Flag is present in YouTube 19.16, but was not used until YouTube 19.43.
// Related issue: https://github.com/inotia00/ReVanced_Extended/issues/2784
internal const val SEARCH_FRAGMENT_FEATURE_FLAG = 45353159L

internal val searchFragmentFeatureFlagFingerprint = legacyFingerprint(
    name = "searchFragmentFeatureFlagFingerprint",
    literals = listOf(SEARCH_FRAGMENT_FEATURE_FLAG),
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

internal val searchSuggestionEndpointFingerprint = legacyFingerprint(
    name = "searchSuggestionEndpointFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    customFingerprint = { method, _ ->
        indexOfIsEmptyInstruction(method) >= 0
    }
)

internal fun indexOfIsEmptyInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_STATIC &&
                getReference<MethodReference>()?.toString() == "Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z"
    }

/**
 * Matches using the class found in [searchSuggestionEndpointFingerprint].
 */
internal val searchSuggestionEndpointParentFingerprint = legacyFingerprint(
    name = "searchSuggestionEndpointParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    strings = listOf("\u2026 "),
)

/**
 * This fingerprint is compatible with versions prior to 19.46,
 * but the 'You may like' section will only appear in 19.46 and later.
 * This fingerprint is not compatible with 20.15+.
 */
internal val searchSuggestionCollectionFingerprint = legacyFingerprint(
    name = "searchSuggestionCollectionFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/util/Collection;", "Ljava/lang/String;"),
    literals = listOf(p13nHeader, seeMoreProceedingHeader)
)

/**
 * YouTube 19.47 ~ 20.14.
 */
internal const val ROUND_EDGE_SEARCH_BAR_FEATURE_FLAG = 45353159L

internal val roundEdgeSearchBarFeatureFlagFingerprint = legacyFingerprint(
    name = "searchFragmentFeatureFlagFingerprint",
    literals = listOf(ROUND_EDGE_SEARCH_BAR_FEATURE_FLAG),
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

internal val toolbarSearchButtonFingerprint = legacyFingerprint(
    name = "toolbarSearchButtonFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/view/MenuItem;"),
    customFingerprint = { method, _ ->
        indexOfShowAsActionInstruction(method) >= 0
    }
)

internal fun indexOfShowAsActionInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        getReference<MethodReference>()?.name == "setShowAsAction"
    }

internal val toolbarSearchButtonLabelFingerprint = legacyFingerprint(
    name = "toolbarSearchButtonLabelFingerprint",
    returnType = "Ljava/lang/CharSequence;",
    literals = listOf(menuSearch),
)

internal val voiceInputControllerParentFingerprint = legacyFingerprint(
    name = "voiceInputControllerParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("[B", "Z"),
    strings = listOf("VoiceInputController"),
)

internal val voiceInputControllerFingerprint = legacyFingerprint(
    name = "voiceInputControllerFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "resolveActivity"
        } >= 0
    },
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
