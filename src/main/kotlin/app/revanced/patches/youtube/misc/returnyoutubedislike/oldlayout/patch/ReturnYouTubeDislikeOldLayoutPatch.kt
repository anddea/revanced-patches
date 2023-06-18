package app.revanced.patches.youtube.misc.returnyoutubedislike.oldlayout.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.dislikeButtonId
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.likeButtonId
import app.revanced.patches.youtube.misc.returnyoutubedislike.oldlayout.fingerprints.*
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.UTILS_PATH
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.reference.Reference

@Name("return-youtube-dislike-old-layout")
@DependsOn([SharedResourceIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class ReturnYouTubeDislikeOldLayoutPatch : BytecodePatch(
    listOf(
        SlimMetadataButtonParentFingerprint,
        ButtonTagFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        SlimMetadataButtonParentFingerprint.result?.let { parentResult ->

            SlimMetadataButtonViewFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    slimMetadataButtonViewFieldReference =
                        getInstruction<ReferenceInstruction>(startIndex).reference
                }
            } ?: return SlimMetadataButtonViewFingerprint.toErrorResult()

            SlimMetadataButtonTextFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.startIndex
                    val setTextInstruction = getInstruction<FiveRegisterInstruction>(insertIndex)

                    val tempRegister =
                        setTextInstruction.registerC + 1

                    val charSequenceRegister =
                        setTextInstruction.registerD

                    addInstructions(
                        insertIndex, """
                            iget-object v$tempRegister, p0, $slimMetadataButtonViewFieldReference
                            invoke-static {v$tempRegister, v$charSequenceRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->onSetText(Landroid/view/View;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                            move-result-object v$charSequenceRegister
                            """
                    )
                }
            } ?: return SlimMetadataButtonTextFingerprint.toErrorResult()
        } ?: return SlimMetadataButtonParentFingerprint.toErrorResult()


        ButtonTagFingerprint.result?.let { parentResult ->

            ButtonTagOnClickFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    getActiveBooleanFieldReference =
                        getInstruction<ReferenceInstruction>(startIndex).reference
                }
            } ?: return ButtonTagOnClickFingerprint.toErrorResult()

            parentResult.mutableMethod.apply {
                val dislikeButtonIndex = getWideLiteralIndex(dislikeButtonId)
                val dislikeButtonRegister = getInstruction<OneRegisterInstruction>(dislikeButtonIndex).registerA
                val dislikeButtonInstruction = getInstruction<TwoRegisterInstruction>(dislikeButtonIndex - 1)

                addInstructions(
                    dislikeButtonIndex, """
                        invoke-virtual {v${dislikeButtonInstruction.registerB}}, $getActiveBooleanFieldReference
                        move-result v$dislikeButtonRegister
                        invoke-static {v${dislikeButtonInstruction.registerA}, v$dislikeButtonRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->setDislikeTag(Landroid/view/View;Z)V
                        """
                )

                val likeButtonIndex = getWideLiteralIndex(likeButtonId)
                val likeButtonRegister = getInstruction<OneRegisterInstruction>(likeButtonIndex).registerA
                val likeButtonInstruction = getInstruction<TwoRegisterInstruction>(likeButtonIndex - 1)

                addInstructions(
                    likeButtonIndex, """
                        invoke-virtual {v${likeButtonInstruction.registerB}}, $getActiveBooleanFieldReference
                        move-result v$likeButtonRegister
                        invoke-static {v${likeButtonInstruction.registerA}, v$likeButtonRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->setLikeTag(Landroid/view/View;Z)V
                        """
                )
            }
        } ?: return ButtonTagFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
    private companion object {
        const val INTEGRATIONS_RYD_CLASS_DESCRIPTOR =
            "$UTILS_PATH/ReturnYouTubeDislikePatch;"

        lateinit var slimMetadataButtonViewFieldReference: Reference
        lateinit var getActiveBooleanFieldReference: Reference
    }
}
