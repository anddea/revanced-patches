package app.revanced.patches.youtube.utils.navigation

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.fingerprints.InitializeButtonsFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.SHARED_PATH
import app.revanced.patches.youtube.utils.mainactivity.MainActivityResolvePatch
import app.revanced.patches.youtube.utils.navigation.fingerprints.NavigationEnumFingerprint
import app.revanced.patches.youtube.utils.navigation.fingerprints.PivotBarButtonsCreateDrawableViewFingerprint
import app.revanced.patches.youtube.utils.navigation.fingerprints.PivotBarButtonsCreateResourceViewFingerprint
import app.revanced.patches.youtube.utils.navigation.fingerprints.PivotBarButtonsViewSetSelectedFingerprint
import app.revanced.patches.youtube.utils.navigation.fingerprints.PivotBarConstructorFingerprint
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.getReference
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

@Patch(
    description = "Hooks the active navigation or search bar.",
    dependencies = [
        MainActivityResolvePatch::class,
        PlayerTypeHookPatch::class,
        SharedResourceIdPatch::class
    ],
)
@Suppress("unused")
object NavigationBarHookPatch : BytecodePatch(
    setOf(
        NavigationEnumFingerprint,
        PivotBarButtonsCreateDrawableViewFingerprint,
        PivotBarButtonsCreateResourceViewFingerprint,
        PivotBarButtonsViewSetSelectedFingerprint,
        PivotBarConstructorFingerprint
    ),
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$SHARED_PATH/NavigationBar;"
    private const val INTEGRATIONS_NAVIGATION_BUTTON_DESCRIPTOR =
        "$SHARED_PATH/NavigationBar\$NavigationButton;"

    private lateinit var navigationTabCreatedCallback: MutableMethod

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
            resolve(context, PivotBarConstructorFingerprint.resultOrThrow().classDef)
        }.resultOrThrow().mutableMethod.apply {
            // Hook the current navigation bar enum value. Note, the 'You' tab does not have an enum value.
            val navigationEnumClassName = NavigationEnumFingerprint.resultOrThrow().mutableClass.type
            addHook(Hook.SET_LAST_APP_NAVIGATION_ENUM) {
                opcode == Opcode.INVOKE_STATIC &&
                        getReference<MethodReference>()?.definingClass == navigationEnumClassName
            }

            // Hook the creation of navigation tab views.
            val drawableTabMethod = PivotBarButtonsCreateDrawableViewFingerprint.resultOrThrow().mutableMethod
            addHook(Hook.NAVIGATION_TAB_LOADED) predicate@{
                MethodUtil.methodSignaturesMatch(
                    getReference<MethodReference>() ?: return@predicate false,
                    drawableTabMethod,
                )
            }

            val imageResourceTabMethod = PivotBarButtonsCreateResourceViewFingerprint.resultOrThrow().method
            addHook(Hook.NAVIGATION_IMAGE_RESOURCE_TAB_LOADED) predicate@{
                MethodUtil.methodSignaturesMatch(
                    getReference<MethodReference>() ?: return@predicate false,
                    imageResourceTabMethod,
                )
            }
        }

        PivotBarButtonsViewSetSelectedFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val index = it.scanResult.patternScanResult!!.startIndex + 1
                val instruction = getInstruction<FiveRegisterInstruction>(index)
                val viewRegister = instruction.registerC
                val isSelectedRegister = instruction.registerD

                addInstruction(
                    index + 1,
                    "invoke-static { v$viewRegister, v$isSelectedRegister }, " +
                            "$INTEGRATIONS_CLASS_DESCRIPTOR->navigationTabSelected(Landroid/view/View;Z)V",
                )
            }
        }

        navigationTabCreatedCallback = context.findClass(INTEGRATIONS_CLASS_DESCRIPTOR)?.mutableClass?.methods?.first { method ->
            method.name == "navigationTabCreatedCallback"
        } ?: throw PatchException("Could not find navigationTabCreatedCallback method")

        MainActivityResolvePatch.injectOnBackPressedMethodCall(INTEGRATIONS_CLASS_DESCRIPTOR, "onBackPressed")
    }

    val hookNavigationButtonCreated: (String) -> Unit by lazy {
        navigationTabCreatedCallback
        { integrationsClassDescriptor ->
            navigationTabCreatedCallback.addInstruction(
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