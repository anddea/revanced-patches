package app.revanced.patches.youtube.shorts.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsTimeStampConstructorFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsTimeStampMetaPanelFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsTimeStampPrimaryFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsTimeStampSecondaryFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.SHORTS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.MetaPanel
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelVodTimeStampsContainer
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.literalInstructionViewHook
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

object ShortsTimeStampPatch : BytecodePatch(
    setOf(
        ShortsTimeStampConstructorFingerprint,
        ShortsTimeStampMetaPanelFingerprint,
        ShortsTimeStampPrimaryFingerprint,
        ShortsTimeStampSecondaryFingerprint,
    )
) {
    override fun execute(context: BytecodeContext) {

        if (!SettingsPatch.upward1925) return

        // region patch for enable time stamp

        mapOf(
            ShortsTimeStampPrimaryFingerprint to 45627350,
            ShortsTimeStampPrimaryFingerprint to 45638282,
            ShortsTimeStampSecondaryFingerprint to 45638187
        ).forEach { (fingerprint, literal) ->
            fingerprint.literalInstructionBooleanHook(
                literal,
                "$SHORTS_CLASS_DESCRIPTOR->enableShortsTimeStamp(Z)Z"
            )
        }

        ShortsTimeStampPrimaryFingerprint.resultOrThrow().mutableMethod.apply {
            val literalIndex = getWideLiteralInstructionIndex(10002)
            val literalRegister = getInstruction<OneRegisterInstruction>(literalIndex).registerA

            addInstructions(
                literalIndex + 1, """
                    invoke-static {v$literalRegister}, $SHORTS_CLASS_DESCRIPTOR->enableShortsTimeStamp(I)I
                    move-result v$literalRegister
                    """
            )
        }

        // endregion

        // region patch for timestamp long press action and meta panel bottom margin

        ShortsTimeStampMetaPanelFingerprint.resolve(
            context,
            ShortsTimeStampConstructorFingerprint.resultOrThrow().classDef
        )

        listOf(
            Triple(
                ShortsTimeStampConstructorFingerprint,
                ReelVodTimeStampsContainer,
                "setShortsTimeStampChangeRepeatState"
            ),
            Triple(
                ShortsTimeStampMetaPanelFingerprint,
                MetaPanel,
                "setShortsMetaPanelBottomMargin"
            )
        ).forEach { (fingerprint, literalValue, methodName) ->
            val smaliInstruction = """
                invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $SHORTS_CLASS_DESCRIPTOR->$methodName(Landroid/view/View;)V
                """

            fingerprint.literalInstructionViewHook(literalValue, smaliInstruction)
        }

        // endregion

    }
}