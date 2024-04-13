package app.revanced.patches.youtube.navigation.navigationbuttons

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstructions
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.MainActivityOnBackPressedFingerprint
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.PivotBarButtonsViewSetSelectedFingerprint
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.*
import app.revanced.util.exception
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

@Patch(
    description = "Hooks the active navigation or search bar.",
    dependencies = [
        NavigationBarHookResourcePatch::class,
    ],
)
@Suppress("unused")
object NavigationBarHookPatch : BytecodePatch(
    setOf(
        PivotBarConstructorFingerprint,
        NavigationEnumFingerprint,
        PivotBarButtonsCreateDrawableViewFingerprint,
        PivotBarButtonsCreateResourceViewFingerprint,
        PivotBarButtonsViewSetSelectedFingerprint,
        NavigationBarHookCallbackFingerprint,
        MainActivityOnBackPressedFingerprint,
        ActionBarSearchResultsFingerprint,
    ),
) {
    internal const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "Lapp/revanced/integrations/youtube/shared/NavigationBar;"

    internal const val INTEGRATIONS_NAVIGATION_BUTTON_DESCRIPTOR =
        "Lapp/revanced/integrations/youtube/shared/NavigationBar\$NavigationButton;"

    private fun MethodFingerprint.getResultOrThrow() =
        result ?: throw exception

    override fun execute(context: BytecodeContext) {
        fun MutableMethod.addHook(hook: Hook, insertPredicate: Instruction.() -> Boolean) {
            val filtered = getInstructions().filter(insertPredicate)
            if (filtered.isEmpty()) throw PatchException("Could not find insert indexes")
            filtered.forEach {
                val insertIndex = it.location.index + 2
                val register = getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static { v$register }, " +
                            "$INTEGRATIONS_CLASS_DESCRIPTOR->${hook.methodName}(${hook.parameters})V",
                )
            }
        }

        InitializeButtonsFingerprint.apply {
            resolve(context, PivotBarConstructorFingerprint.getResultOrThrow().classDef)
        }.getResultOrThrow().mutableMethod.apply {
            // Hook the current navigation bar enum value. Note, the 'You' tab does not have an enum value.
            val navigationEnumClassName = NavigationEnumFingerprint.getResultOrThrow().mutableClass.type
            addHook(Hook.SET_LAST_APP_NAVIGATION_ENUM) {
                opcode == Opcode.INVOKE_STATIC &&
                    getReference<MethodReference>()?.definingClass == navigationEnumClassName
            }

            // Hook the creation of navigation tab views.
            val drawableTabMethod = PivotBarButtonsCreateDrawableViewFingerprint.getResultOrThrow().mutableMethod
            addHook(Hook.NAVIGATION_TAB_LOADED) predicate@{
                MethodUtil.methodSignaturesMatch(
                    getReference<MethodReference>() ?: return@predicate false,
                    drawableTabMethod,
                )
            }

            val imageResourceTabMethod = PivotBarButtonsCreateResourceViewFingerprint.getResultOrThrow().method
            addHook(Hook.NAVIGATION_IMAGE_RESOURCE_TAB_LOADED) predicate@{
                MethodUtil.methodSignaturesMatch(
                    getReference<MethodReference>() ?: return@predicate false,
                    imageResourceTabMethod,
                )
            }
        }

        PivotBarButtonsViewSetSelectedFingerprint.getResultOrThrow().mutableMethod.apply {
            val index = PivotBarButtonsViewSetSelectedFingerprint.indexOfSetViewSelectedInstruction(this)
            val instruction = getInstruction<FiveRegisterInstruction>(index)
            val viewRegister = instruction.registerC
            val isSelectedRegister = instruction.registerD

            addInstruction(
                index + 1,
                "invoke-static { v$viewRegister, v$isSelectedRegister }, " +
                    "$INTEGRATIONS_CLASS_DESCRIPTOR->navigationTabSelected(Landroid/view/View;Z)V",
            )
        }

        // Hook onto back button pressed.  Needed to fix race problem with
        // litho filtering based on navigation tab before the tab is updated.
        MainActivityOnBackPressedFingerprint.getResultOrThrow().mutableMethod.apply {
            addInstruction(
                0,
                "invoke-static { p0 }, " +
                    "$INTEGRATIONS_CLASS_DESCRIPTOR->onBackPressed(Landroid/app/Activity;)V",
            )
        }

        // Hook the search bar.

        // Two different layouts are used at the hooked code.
        // Insert before the first ViewGroup method call after inflating,
        // so this works regardless which layout is used.
        ActionBarSearchResultsFingerprint.getResultOrThrow().mutableMethod.apply {
            val instructionIndex = indexOfFirstInstruction {
                opcode == Opcode.INVOKE_VIRTUAL && getReference<MethodReference>()?.name == "setLayoutDirection"
            }

            val viewRegister = getInstruction<FiveRegisterInstruction>(instructionIndex).registerC

            addInstruction(
                instructionIndex,
                "invoke-static { v$viewRegister }, " +
                    "$INTEGRATIONS_CLASS_DESCRIPTOR->searchBarResultsViewLoaded(Landroid/view/View;)V",
            )
        }
    }

    val hookNavigationButtonCreated: (String) -> Unit by lazy {
        val method = NavigationBarHookCallbackFingerprint.getResultOrThrow().mutableMethod

        { integrationsClassDescriptor ->
            method.addInstruction(
                0,
                "invoke-static { p0, p1 }, " +
                    "$integrationsClassDescriptor->navigationTabCreated" +
                    "(${INTEGRATIONS_NAVIGATION_BUTTON_DESCRIPTOR}Landroid/view/View;)V",
            )
        }
    }

    private enum class Hook(val methodName: String, val parameters: String) {
        SET_LAST_APP_NAVIGATION_ENUM("setLastAppNavigationEnum", "Ljava/lang/Enum;"),
        NAVIGATION_TAB_LOADED("navigationTabLoaded", "Landroid/view/View;"),
        NAVIGATION_IMAGE_RESOURCE_TAB_LOADED("navigationImageResourceTabLoaded", "Landroid/view/View;"),
        SEARCH_BAR_RESULTS_VIEW_LOADED("searchBarResultsViewLoaded", "Landroid/view/View;"),
    }
}
