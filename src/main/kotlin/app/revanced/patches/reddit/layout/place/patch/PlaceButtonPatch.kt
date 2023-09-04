package app.revanced.patches.reddit.layout.place.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.reddit.layout.place.fingerprints.HomePagerScreenFingerprint
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.reddit.utils.settings.bytecode.patch.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getStringIndex
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Hide place button")
@Description("Hide r/place button in toolbar.")
@DependsOn([SettingsPatch::class])
@RedditCompatibility
class PlaceButtonPatch : BytecodePatch(
    listOf(HomePagerScreenFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        HomePagerScreenFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex =
                    getStringIndex("view.findViewById(Search\u2026nav_search_cta_container)")
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

    companion object {
        const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/patches/PlaceButtonPatch;" +
                    "->hidePlaceButton(Landroid/view/View;)V"
    }
}
