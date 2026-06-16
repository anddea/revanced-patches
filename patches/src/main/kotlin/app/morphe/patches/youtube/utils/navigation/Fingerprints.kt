package app.morphe.patches.youtube.utils.navigation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import app.morphe.patches.youtube.general.navigation.navigationBarComponentsPatch
import app.morphe.patches.youtube.utils.YOUTUBE_PIVOT_BAR_CLASS_TYPE
import app.morphe.patches.youtube.utils.resourceid.bottomBarContainer
import app.morphe.patches.youtube.utils.resourceid.imageOnlyTab
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object ToolbarLayoutFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "toolbar_container"),
        checkCast("Lcom/google/android/apps/youtube/app/ui/actionbar/MainCollapsingToolbarLayout;")
    )
)

/**
 * Matches to https://android.googlesource.com/platform/frameworks/support/+/9eee6ba/v7/appcompat/src/android/support/v7/widget/Toolbar.java#963
 */
internal object AppCompatToolbarBackButtonFingerprint : Fingerprint(
    definingClass = "Landroid/support/v7/widget/Toolbar;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/graphics/drawable/Drawable;",
    parameters = listOf()
)

internal object InitializeButtonsFingerprint : Fingerprint(
    classFingerprint = PivotBarConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        string("FEvideo_picker")
    )
)

/**
 * Extension method, used for callback into to other patches.
 * Specifically, [navigationBarHookPatch].
 */
internal object NavigationBarHookCallbackFingerprint : Fingerprint(
    definingClass = EXTENSION_CLASS,
    name ="navigationTabCreatedCallback",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf(EXTENSION_NAVIGATION_BUTTON_CLASS, "Landroid/view/View;")
)

/**
 * Matches to the Enum class that looks up ordinal -> instance.
 */
internal object NavigationEnumFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    strings = listOf(
        "PIVOT_HOME",
        "TAB_SHORTS",
        "CREATION_TAB_LARGE",
        "PIVOT_SUBSCRIPTIONS",
        "TAB_ACTIVITY",
        "VIDEO_LIBRARY_WHITE",
        "INCOGNITO_CIRCLE",
    ),
    custom = { _, classDef ->
        // Don't match our own code.
        !classDef.type.startsWith("Lapp/morphe")
    }
)

internal object PivotBarButtonsCreateDrawableViewFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/view/View;",
    custom = { method, _ ->
        // Only one view creation method has a Drawable parameter.
        method.parameterTypes.firstOrNull() == "Landroid/graphics/drawable/Drawable;"
    }
)

internal object PivotBarButtonsCreateResourceStyledViewFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/view/View;",
    parameters = listOf("L", "Z", "I", "L")
)

/**
 * 20.21+
 */
internal object PivotBarButtonsCreateResourceIntViewFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/view/View;",
    custom = { method, _ ->
        // Only one view creation method has an int first parameter.
        method.parameterTypes.firstOrNull() == "I"
    }
)

internal object PivotBarButtonsViewSetSelectedFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("I", "Z"),
    filters = listOf(
        methodCall(name = "setSelected")
    )
)

internal object PivotBarConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        string("com.google.android.apps.youtube.app.endpoint.flags"),
    )
)

internal object ImageEnumConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        string("TAB_ACTIVITY_CAIRO"),
        opcode(Opcode.SPUT_OBJECT)
    ),
    custom = { _, classDef ->
        // Don't match our extension code.
        !classDef.type.startsWith("Lapp/morphe/")
    }
)

internal object SetEnumMapFingerprint : Fingerprint(
    filters = listOf(
        resourceLiteral(ResourceType.DRAWABLE, "yt_fill_bell_black_24"),
        methodCall(smali = "Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;", location = MatchAfterWithin(
            10
        )
        ),
        methodCall(smali = "Ljava/util/EnumMap;->put(Ljava/lang/Enum;Ljava/lang/Object;)Ljava/lang/Object;", location = MatchAfterWithin(
            10
        )
        )
    )
)

internal object InitializeBottomBarContainerFingerprint : Fingerprint(
    name = "run",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.ID, "bottom_bar_container"),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = $$"Landroid/view/View;->addOnLayoutChangeListener(Landroid/view/View$OnLayoutChangeListener;)V"
        )
    )
)

internal const val YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE = "Lcom/google/android/apps/youtube/app/watchwhile/MainActivity;"

internal object YouTubeMainActivityOnBackPressedFingerprint : Fingerprint(
    definingClass = YOUTUBE_MAIN_ACTIVITY_CLASS_TYPE,
    name = "onBackPressed",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_SUPER,
            name = "onBackPressed"
        ),
        opcode(Opcode.RETURN_VOID)
    )
)

internal object ActionBarSearchResultsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Landroid/view/View;",
    filters = listOf(
        resourceLiteral(ResourceType.LAYOUT, "action_bar_search_results_view_mic"),
        methodCall(smali = "Landroid/view/View;->setLayoutDirection(I)V"),
        resourceLiteral(ResourceType.ID, "search_query"),
        checkCast(
            type = "Landroid/widget/TextView;",
            location = MatchAfterWithin(5)
        )
    )
)

internal val initializeBottomBarContainerFingerprint = legacyFingerprint(
    name = "initializeBottomBarContainerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(bottomBarContainer),
    customFingerprint = { method, classDef ->
        AccessFlags.SYNTHETIC.isSet(classDef.accessFlags) &&
                indexOfLayoutChangeListenerInstruction(method) >= 0
    },
)

internal fun indexOfLayoutChangeListenerInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.toString() == "Landroid/view/View;->addOnLayoutChangeListener(Landroid/view/View${'$'}OnLayoutChangeListener;)V"
    }

internal val initializeButtonsFingerprint = legacyFingerprint(
    name = "initializeButtonsFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(imageOnlyTab),
)

/**
 * Extension method, used for callback into to other patches.
 * Specifically, [navigationBarComponentsPatch].
 */
internal val navigationBarHookCallbackFingerprint = legacyFingerprint(
    name = "navigationBarHookCallbackFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    returnType = "V",
    parameters = listOf(EXTENSION_NAVIGATION_BUTTON_DESCRIPTOR, "Landroid/view/View;"),
    customFingerprint = { method, _ ->
        method.name == "navigationTabCreatedCallback" &&
                method.definingClass == EXTENSION_CLASS_DESCRIPTOR
    }
)

/**
 * Resolves to the Enum class that looks up ordinal -> instance.
 */
internal val navigationEnumFingerprint = legacyFingerprint(
    name = "navigationEnumFingerprint",
    accessFlags = AccessFlags.STATIC or AccessFlags.CONSTRUCTOR,
    strings = listOf(
        "PIVOT_HOME",
        "TAB_SHORTS",
        "CREATION_TAB_LARGE",
        "PIVOT_SUBSCRIPTIONS",
        "TAB_ACTIVITY",
        "VIDEO_LIBRARY_WHITE",
        "INCOGNITO_CIRCLE"
    )
)

internal val pivotBarButtonsCreateDrawableViewFingerprint = legacyFingerprint(
    name = "pivotBarButtonsCreateDrawableViewFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    // Method has different number of parameters in some app targets.
    // Parameters are checked in custom fingerprint.
    returnType = "Landroid/view/View;",
    customFingerprint = { method, classDef ->
        classDef.type == YOUTUBE_PIVOT_BAR_CLASS_TYPE &&
                // Only one method has a Drawable parameter.
                method.parameterTypes.firstOrNull() == "Landroid/graphics/drawable/Drawable;"
    }
)

/**
 * 20.21 - 20.27?
 */
internal val pivotBarButtonsCreateResourceIntViewFingerprint = legacyFingerprint(
    name = "pivotBarButtonsCreateResourceIntViewFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Landroid/view/View;",
    customFingerprint = { method, classDef ->
        classDef.type == YOUTUBE_PIVOT_BAR_CLASS_TYPE &&
                // Only one view creation method has an int first parameter.
                method.parameterTypes.firstOrNull() == "I"
    }
)


internal val pivotBarButtonsCreateResourceViewFingerprint = legacyFingerprint(
    name = "pivotBarButtonsCreateResourceViewFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Z", "I", "L"),
    returnType = "Landroid/view/View;",
    customFingerprint = { _, classDef ->
        classDef.type == YOUTUBE_PIVOT_BAR_CLASS_TYPE
    }
)

internal fun indexOfSetViewSelectedInstruction(method: Method) = method.indexOfFirstInstruction {
    opcode == Opcode.INVOKE_VIRTUAL && getReference<MethodReference>()?.name == "setSelected"
}

internal val pivotBarButtonsViewSetSelectedFingerprint = legacyFingerprint(
    name = "pivotBarButtonsViewSetSelectedFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I", "Z"),
    customFingerprint = { method, _ ->
        indexOfSetViewSelectedInstruction(method) >= 0 &&
                method.definingClass == YOUTUBE_PIVOT_BAR_CLASS_TYPE
    }
)

internal val pivotBarConstructorFingerprint = legacyFingerprint(
    name = "pivotBarConstructorFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    strings = listOf("com.google.android.apps.youtube.app.endpoint.flags")
)
