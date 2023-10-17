package app.revanced.patches.reddit.layout.place

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.reddit.layout.place.fingerprints.HomePagerScreenFingerprint
import app.revanced.patches.reddit.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.reddit.utils.resourceid.SharedResourceIdPatch.ToolBarNavSearchCtaContainer
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Hide place button",
    description = "Hide r/place button in toolbar.",
    dependencies =
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [CompatiblePackage("com.reddit.frontpage")]
)
@Suppress("unused")
object PlaceButtonPatch : BytecodePatch(
    setOf(HomePagerScreenFingerprint)
) {
    private const val INTEGRATIONS_METHOD_DESCRIPTOR =
        "Lapp/revanced/reddit/patches/PlaceButtonPatch;" +
                "->hidePlaceButton(Landroid/view/View;)V"

    override fun execute(context: BytecodeContext) {

        HomePagerScreenFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex =
                    getWideLiteralIndex(ToolBarNavSearchCtaContainer) + 3
                val targetRegister =
                    getInstruction<OneRegisterInstruction>(targetIndex - 1).registerA

                addInstruction(
                    targetIndex,
                    "invoke-static {v$targetRegister}, $INTEGRATIONS_METHOD_DESCRIPTOR"
                )
            }
        } ?: throw HomePagerScreenFingerprint.exception

        updateSettingsStatus("PlaceButton")

    }
}
