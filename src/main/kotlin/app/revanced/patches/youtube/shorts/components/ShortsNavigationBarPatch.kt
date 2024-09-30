package app.revanced.patches.youtube.shorts.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.shorts.components.fingerprints.BottomBarContainerHeightFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ReelWatchPagerFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.SHORTS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.navigation.NavigationBarHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.BottomBarContainer
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelWatchPlayer
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.fingerprint.MultiMethodFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstWideLiteralInstructionValue
import app.revanced.util.injectLiteralInstructionViewCall
import app.revanced.util.patch.MultiMethodBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Up to YouTube 19.28.42, there are two Methods with almost the same pattern.
 *
 * In certain YouTube versions, the hook should be done not on the first matching Method, but also on the last matching Method.
 *
 * 'Multiple fingerprint search' feature is not yet implemented in ReVanced Patcher,
 * So I just implement it via [MultiMethodFingerprint].
 *
 * Related Issues:
 * https://github.com/ReVanced/revanced-patcher/issues/74
 * https://github.com/ReVanced/revanced-patcher/issues/308
 */
@Patch(dependencies = [NavigationBarHookPatch::class])
object ShortsNavigationBarPatch : MultiMethodBytecodePatch(
    fingerprints = setOf(ReelWatchPagerFingerprint),
    multiFingerprints = setOf(BottomBarContainerHeightFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        // region patch for set navigation bar height.

        BottomBarContainerHeightFingerprint.resultOrThrow().forEach {
            it.mutableMethod.apply {
                val constIndex = indexOfFirstWideLiteralInstructionValue(BottomBarContainer)

                val targetIndex = indexOfFirstInstructionOrThrow(constIndex) {
                    getReference<MethodReference>()?.name == "getHeight"
                } + 1

                val heightRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$heightRegister}, $SHORTS_CLASS_DESCRIPTOR->overrideNavigationBarHeight(I)I
                        move-result v$heightRegister
                        """
                )
            }
        }

        NavigationBarHookPatch.addBottomBarContainerHook("$SHORTS_CLASS_DESCRIPTOR->setNavigationBar(Landroid/view/View;)V")

        // endregion.

        // region patch for addOnAttachStateChangeListener.

        val smaliInstruction = """
                invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $SHORTS_CLASS_DESCRIPTOR->onShortsCreate(Landroid/view/View;)V
                """

        ReelWatchPagerFingerprint.injectLiteralInstructionViewCall(
            ReelWatchPlayer,
            smaliInstruction
        )

        // endregion.

    }
}