package app.revanced.patches.youtube.navigation.navigationbuttons

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.AutoMotiveFingerprint
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.PivotBarEnumFingerprint
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.PivotBarShortsButtonViewFingerprint
import app.revanced.patches.youtube.utils.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.NAVIGATION
import app.revanced.util.pivotbar.InjectionUtils.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.pivotbar.InjectionUtils.injectHook
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Hide navigation buttons",
    description = "Adds options to hide or change navigation buttons.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39"
            ]
        )
    ]
)
@Suppress("unused")
object NavigationButtonsPatch : BytecodePatch(
    setOf(
        AutoMotiveFingerprint,
        PivotBarCreateButtonViewFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        PivotBarCreateButtonViewFingerprint.result?.let { parentResult ->

            /**
             * Home, Shorts, Subscriptions Button
             */
            with(
                arrayOf(
                    PivotBarEnumFingerprint,
                    PivotBarShortsButtonViewFingerprint
                ).onEach {
                    it.resolve(
                        context,
                        parentResult.mutableMethod,
                        parentResult.mutableClass
                    )
                }.map {
                    it.result?.scanResult?.patternScanResult ?: throw it.exception
                }
            ) {
                val enumScanResult = this[0]
                val buttonViewResult = this[1]

                val enumHookInsertIndex = enumScanResult.startIndex + 2
                val buttonHookInsertIndex = buttonViewResult.endIndex

                mapOf(
                    BUTTON_HOOK to buttonHookInsertIndex,
                    ENUM_HOOK to enumHookInsertIndex
                ).forEach { (hook, insertIndex) ->
                    parentResult.mutableMethod.injectHook(hook, insertIndex)
                }
            }

            /**
             * Create Button
             */
            parentResult.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.let {
                    val scanStart = parentResult.scanResult.patternScanResult!!.endIndex

                    scanStart + it.subList(scanStart, it.size - 1).indexOfFirst { instruction ->
                        instruction.opcode == Opcode.INVOKE_STATIC
                    }
                }
                injectHook(CREATE_BUTTON_HOOK, insertIndex)
            }

        } ?: throw PivotBarCreateButtonViewFingerprint.exception

        /**
         * Switch create button with notifications button
         */
        AutoMotiveFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val register = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$register}, $NAVIGATION->switchCreateNotification(Z)Z
                        move-result v$register
                        """
                )
            }
        } ?: throw AutoMotiveFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: HIDE_NAVIGATION_BUTTONS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-navigation-buttons")

    }

    private const val ENUM_HOOK =
        "sput-object v$REGISTER_TEMPLATE_REPLACEMENT, $NAVIGATION" +
                "->" +
                "lastPivotTab:Ljava/lang/Enum;"

    private const val BUTTON_HOOK =
        "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $NAVIGATION" +
                "->" +
                "hideNavigationButton(Landroid/view/View;)V"

    private const val CREATE_BUTTON_HOOK =
        "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $NAVIGATION" +
                "->" +
                "hideCreateButton(Landroid/view/View;)V"
}