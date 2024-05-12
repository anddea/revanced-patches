package app.revanced.patches.youtube.utils.settings

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.integrations.Constants.INTEGRATIONS_UTILS_CLASS_DESCRIPTOR
import app.revanced.patches.shared.mapping.ResourceMappingPatch
import app.revanced.patches.youtube.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.mainactivity.MainActivityResolvePatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.fingerprints.ThemeSetterSystemFingerprint
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    dependencies = [
        MainActivityResolvePatch::class,
        ResourceMappingPatch::class,
        SharedResourceIdPatch::class
    ]
)
object SettingsBytecodePatch : BytecodePatch(
    setOf(ThemeSetterSystemFingerprint)
) {
    private const val INTEGRATIONS_INITIALIZATION_CLASS_DESCRIPTOR =
        "$UTILS_PATH/InitializationPatch;"

    private const val INTEGRATIONS_THEME_METHOD_DESCRIPTOR =
        "$INTEGRATIONS_PATH/utils/ThemeUtils;->setTheme(Ljava/lang/Enum;)V"

    internal lateinit var contexts: BytecodeContext

    override fun execute(context: BytecodeContext) {
        contexts = context

        // apply the current theme of the settings page
        ThemeSetterSystemFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                injectCall(implementation!!.instructions.size - 1)
                injectCall(it.scanResult.patternScanResult!!.startIndex)
            }
        }

        MainActivityResolvePatch.injectOnCreateMethodCall(INTEGRATIONS_INITIALIZATION_CLASS_DESCRIPTOR, "setExtendedUtils")
        MainActivityResolvePatch.injectOnCreateMethodCall(INTEGRATIONS_INITIALIZATION_CLASS_DESCRIPTOR, "onCreate")
        MainActivityResolvePatch.injectConstructorMethodCall(INTEGRATIONS_UTILS_CLASS_DESCRIPTOR, "setActivity")

    }
    private fun MutableMethod.injectCall(index: Int) {
        val register = getInstruction<OneRegisterInstruction>(index).registerA

        addInstructions(
            index + 1, """
                invoke-static {v$register}, $INTEGRATIONS_THEME_METHOD_DESCRIPTOR
                return-object v$register
                """
        )
        removeInstruction(index)
    }
}
