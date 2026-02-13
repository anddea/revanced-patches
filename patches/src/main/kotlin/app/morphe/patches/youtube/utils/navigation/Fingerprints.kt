package app.morphe.patches.youtube.utils.navigation

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

