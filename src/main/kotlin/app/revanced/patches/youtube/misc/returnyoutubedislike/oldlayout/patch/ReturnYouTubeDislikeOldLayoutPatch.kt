package app.revanced.patches.youtube.misc.returnyoutubedislike.oldlayout.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.returnyoutubedislike.oldlayout.fingerprints.*
import app.revanced.util.integrations.Constants.UTILS_PATH
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
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
                with (it.mutableMethod) {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    slimMetadataButtonViewFieldReference =
                        (instruction(startIndex) as ReferenceInstruction).reference
                }
            } ?: return SlimMetadataButtonViewFingerprint.toErrorResult()

            SlimMetadataButtonTextFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                with (it.mutableMethod) {
                    val insertIndex = it.scanResult.patternScanResult!!.startIndex
                    val setTextInstruction = instruction(insertIndex) as FiveRegisterInstruction

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
                with (it.mutableMethod) {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    getActiveBooleanFieldReference =
                        (instruction(startIndex) as ReferenceInstruction).reference
                }
            } ?: return ButtonTagOnClickFingerprint.toErrorResult()

            with (parentResult.mutableMethod.implementation!!.instructions) {
                val dislikeButtonIndex = this.indexOfFirst {
                    (it as? WideLiteralInstruction)?.wideLiteral == SharedResourceIdPatch.dislikeButtonLabelId
                }

                val dislikeButtonRegister = (elementAt(dislikeButtonIndex) as OneRegisterInstruction).registerA

                val dislikeButtonInstruction = elementAt(dislikeButtonIndex - 1) as TwoRegisterInstruction

                parentResult.mutableMethod.addInstructions(
                    dislikeButtonIndex, """
                        invoke-virtual {v${dislikeButtonInstruction.registerB}}, $getActiveBooleanFieldReference
                        move-result v$dislikeButtonRegister
                        invoke-static {v${dislikeButtonInstruction.registerA}, v$dislikeButtonRegister}, $INTEGRATIONS_RYD_CLASS_DESCRIPTOR->setDislikeTag(Landroid/view/View;Z)V
                        """
                )

                val likeButtonIndex = this.indexOfFirst {
                    (it as? WideLiteralInstruction)?.wideLiteral == SharedResourceIdPatch.likeButtonLabelId
                }

                val likeButtonRegister = (elementAt(likeButtonIndex) as OneRegisterInstruction).registerA

                val likeButtonInstruction = elementAt(likeButtonIndex - 1) as TwoRegisterInstruction

                parentResult.mutableMethod.addInstructions(
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
