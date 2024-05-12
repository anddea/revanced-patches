package app.revanced.patches.music.general.redirection

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.general.redirection.fingerprints.DislikeButtonOnClickListenerFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.fingerprints.PendingIntentReceiverFingerprint
import app.revanced.patches.music.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference

@Suppress("unused")
object DislikeRedirectionPatch : BaseBytecodePatch(
    name = "Disable dislike redirection",
    description = "Adds an option to disable redirection to the next track when clicking dislike button.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        DislikeButtonOnClickListenerFingerprint,
        PendingIntentReceiverFingerprint
    )
) {
    private lateinit var onClickReference: Reference

    override fun execute(context: BytecodeContext) {

        PendingIntentReceiverFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val startIndex = getStringInstructionIndex("YTM Dislike")
                val onClickRelayIndex = getTargetIndexReversed(startIndex, Opcode.INVOKE_VIRTUAL)
                val onClickRelayMethod = getWalkerMethod(context, onClickRelayIndex)

                onClickRelayMethod.apply {
                    val onClickMethodIndex = getTargetIndexReversed(Opcode.INVOKE_DIRECT)
                    val onClickMethod = getWalkerMethod(context, onClickMethodIndex)

                    onClickMethod.apply {
                        val onClickIndex = indexOfFirstInstruction {
                            val reference = ((this as? ReferenceInstruction)?.reference as? MethodReference)

                            opcode == Opcode.INVOKE_INTERFACE
                                    && reference?.returnType == "V"
                                    && reference.parameterTypes.size == 1
                        }
                        onClickReference = getInstruction<ReferenceInstruction>(onClickIndex).reference

                        injectCall(onClickIndex)
                    }
                }
            }
        }

        DislikeButtonOnClickListenerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val onClickIndex = getTargetIndexWithReference(onClickReference.toString())
                injectCall(onClickIndex)
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_disable_dislike_redirection",
            "false"
        )

    }

    private fun MutableMethod.injectCall(onClickIndex: Int) {
        val targetIndex = getTargetIndexReversed(onClickIndex, Opcode.IF_EQZ)
        val insertRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

        addInstructionsWithLabels(
            targetIndex + 1, """
                invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->disableDislikeRedirection()Z
                move-result v$insertRegister
                if-nez v$insertRegister, :disable
                """, ExternalLabel("disable", getInstruction(onClickIndex + 1))
        )
    }
}
