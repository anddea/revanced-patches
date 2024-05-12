package app.revanced.patches.youtube.general.tabletminiplayer

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.general.tabletminiplayer.fingerprints.MiniPlayerDimensionsCalculatorFingerprint
import app.revanced.patches.youtube.general.tabletminiplayer.fingerprints.MiniPlayerOverrideFingerprint
import app.revanced.patches.youtube.general.tabletminiplayer.fingerprints.MiniPlayerOverrideNoContextFingerprint
import app.revanced.patches.youtube.general.tabletminiplayer.fingerprints.MiniPlayerResponseModelSizeCheckFingerprint
import app.revanced.patches.youtube.general.tabletminiplayer.fingerprints.ModernMiniPlayerConfigFingerprint
import app.revanced.patches.youtube.general.tabletminiplayer.fingerprints.ModernMiniPlayerConstructorFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ModernMiniPlayerForwardButton
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ModernMiniPlayerRewindButton
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.YtOutlinePiPWhite
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.YtOutlineXWhite
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.getReference
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.literalInstructionHook
import app.revanced.util.literalInstructionViewHook
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
object TabletMiniPlayerPatch : BaseBytecodePatch(
    name = "Enable tablet mini player",
    description = "Adds an option to enable the tablet mini player layout.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        MiniPlayerDimensionsCalculatorFingerprint,
        MiniPlayerResponseModelSizeCheckFingerprint,
        MiniPlayerOverrideFingerprint,
        ModernMiniPlayerConfigFingerprint,
        ModernMiniPlayerConstructorFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        MiniPlayerOverrideNoContextFingerprint.resolve(
            context,
            MiniPlayerDimensionsCalculatorFingerprint.resultOrThrow().classDef
        )
        MiniPlayerOverrideNoContextFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                hook(getTargetIndex(Opcode.RETURN))
                hook(getTargetIndexReversed(Opcode.RETURN))
            }
        }

        if (SettingsPatch.upward1912) {
            ModernMiniPlayerConfigFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    hook(it.scanResult.patternScanResult!!.endIndex)
                }
            }

            ModernMiniPlayerConstructorFingerprint.resultOrThrow().let {
                it.mutableClass.methods.forEach { mutableMethod ->
                    mutableMethod.hookModernMiniPlayer()
                }
            }

            // In ModernMiniPlayer, the drawables of the close button and expand button are reversed.
            // OnClickListener appears to be applied normally, so this appears to be a bug in YouTube.
            // To solve this, swap the drawables of the close and expand buttons.
            // This Drawable will be used in multiple Classes, so instead of using LiteralValueFingerprint to patch only specific methods,
            // Apply the patch to all methods where literals are used.
            mapOf(
                YtOutlineXWhite to "replaceCloseButtonDrawableId",
                YtOutlinePiPWhite to "replaceExpandButtonDrawableId"
            ).forEach { (literal, methodName) ->
                val smaliInstruction = """
                    invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $GENERAL_CLASS_DESCRIPTOR->$methodName(I)I
                    move-result v$REGISTER_TEMPLATE_REPLACEMENT
                    """

                context.literalInstructionHook(literal, smaliInstruction)
            }

            arrayOf(
                ModernMiniPlayerForwardButton,
                ModernMiniPlayerRewindButton
            ).forEach { literal ->
                val smaliInstruction = """
                    invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $GENERAL_CLASS_DESCRIPTOR->hideRewindAndForwardButton(Landroid/view/View;)V
                    """

                context.literalInstructionViewHook(literal, smaliInstruction)
            }

            SettingsPatch.addPreference(
                arrayOf(
                    "SETTINGS: ENABLE_MODERN_MINI_PLAYER"
                )
            )
        } else {
            MiniPlayerOverrideFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val walkerMethod = getWalkerMethod(context, getStringInstructionIndex("appName") + 2)

                    walkerMethod.apply {
                        hook(getTargetIndex(Opcode.RETURN))
                        hook(getTargetIndexReversed(Opcode.RETURN))
                    }
                }
            }

            MiniPlayerResponseModelSizeCheckFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    hook(it.scanResult.patternScanResult!!.endIndex)
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: ENABLE_TABLET_MINI_PLAYER"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }

    private fun MutableMethod.hook(index: Int) =
        hook(index, "enableTabletMiniPlayer")

    private fun MutableMethod.hook(
        index: Int,
        methodName: String
    ) {
        val register = getInstruction<OneRegisterInstruction>(index).registerA

        addInstructions(
            index, """
                invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->$methodName(Z)Z
                move-result v$register
                """
        )
    }

    private fun MutableMethod.hookModernMiniPlayer() {
        if (returnType == "Z") {
            hook(getTargetIndexReversed(Opcode.RETURN), "enableModernMiniPlayer")
            hook(getTargetIndex(Opcode.RETURN), "enableModernMiniPlayer")
        }

        val iPutIndex = indexOfFirstInstruction {
            this.opcode == Opcode.IPUT
                    && this.getReference<FieldReference>()?.type == "I"
        }

        if (iPutIndex < 0) return

        val targetReference = getInstruction<ReferenceInstruction>(iPutIndex).reference
        val targetInstruction = getInstruction<TwoRegisterInstruction>(iPutIndex)

        addInstructions(
            iPutIndex + 1, """
                invoke-static {v${targetInstruction.registerA}}, $GENERAL_CLASS_DESCRIPTOR->enableModernMiniPlayer(I)I
                move-result v${targetInstruction.registerA}
                iput v${targetInstruction.registerA}, v${targetInstruction.registerB}, $targetReference
                """
        )
        removeInstruction(iPutIndex)
    }
}
