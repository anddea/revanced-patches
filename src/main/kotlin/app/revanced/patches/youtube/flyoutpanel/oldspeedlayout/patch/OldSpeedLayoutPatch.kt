package app.revanced.patches.youtube.flyoutpanel.oldspeedlayout.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patches.youtube.flyoutpanel.oldspeedlayout.fingerprints.CustomPlaybackSpeedIntegrationsFingerprint
import app.revanced.patches.youtube.flyoutpanel.oldspeedlayout.fingerprints.PlaybackRateBottomSheetBuilderFingerprint
import app.revanced.patches.youtube.flyoutpanel.oldspeedlayout.fingerprints.PlaybackRateBottomSheetClassFingerprint
import app.revanced.patches.youtube.utils.fingerprints.NewFlyoutPanelBuilderFingerprint
import app.revanced.patches.youtube.utils.litho.patch.LithoFilterPatch
import app.revanced.util.integrations.Constants.PATCHES_PATH
import app.revanced.util.integrations.Constants.VIDEO_PATH
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableField

@DependsOn([LithoFilterPatch::class])
class OldSpeedLayoutPatch : BytecodePatch(
    listOf(
        CustomPlaybackSpeedIntegrationsFingerprint,
        NewFlyoutPanelBuilderFingerprint,
        PlaybackRateBottomSheetClassFingerprint,
        PlaybackRateBottomSheetBuilderFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        /**
         * Find the values we need
         */
        PlaybackRateBottomSheetBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                PLAYBACK_RATE_BOTTOM_SHEET_CLASS = definingClass
                PLAYBACK_RATE_BOTTOM_SHEET_BUILDER_METHOD = name
            }
        } ?: return PlaybackRateBottomSheetClassFingerprint.toErrorResult()

        /**
         * Create a static field in the patch
         * Add a call the Playback Speed Bottom Sheet Fragment method
         */
        CustomPlaybackSpeedIntegrationsFingerprint.result?.let {
            it.mutableMethod.apply {
                // Create a static field 'playbackRateBottomSheetClass' in FlyoutPanelPatch.
                it.mutableClass.staticFields.add(
                    ImmutableField(
                        definingClass,
                        "playbackRateBottomSheetClass",
                        PLAYBACK_RATE_BOTTOM_SHEET_CLASS,
                        AccessFlags.PUBLIC or AccessFlags.STATIC,
                        null,
                        annotations,
                        null
                    ).toMutable()
                )

                removeInstruction(1)
                removeInstruction(0)

                addInstructionsWithLabels(
                    0, """
                        sget-object v0, $INTEGRATIONS_CLASS_DESCRIPTOR->playbackRateBottomSheetClass:$PLAYBACK_RATE_BOTTOM_SHEET_CLASS
                        if-nez v0, :not_null
                        return-void
                        :not_null
                        invoke-virtual {v0}, $PLAYBACK_RATE_BOTTOM_SHEET_CLASS->$PLAYBACK_RATE_BOTTOM_SHEET_BUILDER_METHOD()V
                        """
                )
            }
        } ?: return CustomPlaybackSpeedIntegrationsFingerprint.toErrorResult()

        /**
         * Input 'playbackRateBottomSheetClass' in FlyoutPanelPatch.
         */
        PlaybackRateBottomSheetClassFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstruction(
                    0,
                    "sput-object p0, $INTEGRATIONS_CLASS_DESCRIPTOR->playbackRateBottomSheetClass:$PLAYBACK_RATE_BOTTOM_SHEET_CLASS"
                )
            }
        } ?: return PlaybackRateBottomSheetClassFingerprint.toErrorResult()

        /**
         * New method
         */
        NewFlyoutPanelBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static { v$insertRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->onFlyoutMenuCreate(Landroid/widget/LinearLayout;)V"
                )
            }
        } ?: return NewFlyoutPanelBuilderFingerprint.toErrorResult()

        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/PlaybackSpeedMenuFilter;")

        return PatchResultSuccess()
    }

    private companion object {
        private const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$VIDEO_PATH/CustomPlaybackSpeedPatch;"

        lateinit var PLAYBACK_RATE_BOTTOM_SHEET_CLASS: String
        lateinit var PLAYBACK_RATE_BOTTOM_SHEET_BUILDER_METHOD: String
    }
}
