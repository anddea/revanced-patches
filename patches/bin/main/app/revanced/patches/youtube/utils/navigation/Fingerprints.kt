package app.revanced.patches.youtube.utils.navigation

import app.revanced.patches.youtube.general.navigation.navigationBarComponentsPatch
import app.revanced.patches.youtube.utils.resourceid.bottomBarContainer
import app.revanced.patches.youtube.utils.resourceid.imageOnlyTab
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
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
        classDef.type == "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;" &&
                // Only one method has a Drawable parameter.
                method.parameterTypes.firstOrNull() == "Landroid/graphics/drawable/Drawable;"
    }
)

internal val pivotBarButtonsCreateResourceViewFingerprint = legacyFingerprint(
    name = "pivotBarButtonsCreateResourceViewFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Z", "I", "L"),
    returnType = "Landroid/view/View;",
    customFingerprint = { _, classDef ->
        classDef.type == "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;"
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
                method.definingClass == "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;"
    }
)

internal val pivotBarConstructorFingerprint = legacyFingerprint(
    name = "pivotBarConstructorFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    strings = listOf("com.google.android.apps.youtube.app.endpoint.flags")
)

