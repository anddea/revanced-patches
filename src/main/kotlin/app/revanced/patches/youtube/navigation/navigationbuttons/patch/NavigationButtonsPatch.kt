package app.revanced.patches.youtube.navigation.navigationbuttons.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.AutoMotiveFingerprint
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.PivotBarEnumFingerprint
import app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints.PivotBarShortsButtonViewFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.NAVIGATION
import app.revanced.util.pivotbar.InjectionUtils.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.pivotbar.InjectionUtils.injectHook
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Hide navigation buttons")
@Description("Adds options to hide or change navigation buttons.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class NavigationButtonsPatch : BytecodePatch(
    listOf(
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
                    buttonHook to buttonHookInsertIndex,
                    enumHook to enumHookInsertIndex
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
                injectHook(createButtonHook, insertIndex)
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

    private companion object {
        const val enumHook =
            "sput-object v$REGISTER_TEMPLATE_REPLACEMENT, $NAVIGATION" +
                    "->" +
                    "lastPivotTab:Ljava/lang/Enum;"

        const val buttonHook =
            "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $NAVIGATION" +
                    "->" +
                    "hideNavigationButton(Landroid/view/View;)V"

        const val createButtonHook =
            "invoke-static { v$REGISTER_TEMPLATE_REPLACEMENT }, $NAVIGATION" +
                    "->" +
                    "hideCreateButton(Landroid/view/View;)V"
    }
}