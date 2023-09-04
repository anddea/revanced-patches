package app.revanced.patches.youtube.misc.layoutswitch.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.misc.layoutswitch.fingerprints.ClientFormFactorFingerprint
import app.revanced.patches.youtube.misc.layoutswitch.fingerprints.ClientFormFactorParentFingerprint
import app.revanced.patches.youtube.misc.layoutswitch.fingerprints.ClientFormFactorWalkerFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.LayoutSwitchFingerprint
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.Opcode

@Patch
@Name("Layout switch")
@Description("Tricks the dpi to use some tablet/phone layouts.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class LayoutSwitchPatch : BytecodePatch(
    listOf(
        ClientFormFactorParentFingerprint,
        LayoutSwitchFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        fun MutableMethod.injectTabletLayout(jumpIndex: Int) {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $MISC_PATH/LayoutOverridePatch;->enableTabletLayout()Z
                    move-result v0
                    if-nez v0, :tablet_layout
                    """, ExternalLabel("tablet_layout", getInstruction(jumpIndex))
            )
        }

        ClientFormFactorParentFingerprint.result?.classDef?.let { classDef ->
            try {
                ClientFormFactorFingerprint.also { it.resolve(context, classDef) }.result!!.apply {
                    mutableMethod.injectTabletLayout(scanResult.patternScanResult!!.startIndex + 1)
                }
            } catch (_: Exception) {
                ClientFormFactorWalkerFingerprint.also {
                    it.resolve(
                        context,
                        classDef
                    )
                }.result?.let {
                    (context
                        .toMethodWalker(it.method)
                        .nextMethod(it.scanResult.patternScanResult!!.startIndex, true)
                        .getMethod() as MutableMethod).apply {

                        val jumpIndex = implementation!!.instructions.indexOfFirst { instruction ->
                            instruction.opcode == Opcode.RETURN_OBJECT
                        } - 1

                        injectTabletLayout(jumpIndex)
                    }
                } ?: throw ClientFormFactorWalkerFingerprint.exception
            }
        } ?: throw ClientFormFactorParentFingerprint.exception

        LayoutSwitchFingerprint.result?.mutableMethod?.addInstructions(
            4, """
                invoke-static {p0}, $MISC_PATH/LayoutOverridePatch;->getLayoutOverride(I)I
                move-result p0
                """
        ) ?: throw LayoutSwitchFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: LAYOUT_SWITCH"
            )
        )

        SettingsPatch.updatePatchStatus("layout-switch")

    }
}
