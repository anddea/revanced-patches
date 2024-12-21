package app.revanced.patches.youtube.swipe.controls

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.mainactivity.mainActivityMutableClass
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.SWIPE_PATH
import app.revanced.patches.youtube.utils.lockmodestate.lockModeStateHookPatch
import app.revanced.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import app.revanced.patches.youtube.utils.patch.PatchList.SWIPE_CONTROLS
import app.revanced.patches.youtube.utils.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.utils.playservice.is_19_09_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_23_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_36_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.resourceid.autoNavScrollCancelPadding
import app.revanced.patches.youtube.utils.resourceid.fullScreenEngagementOverlay
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.ResourceUtils.getContext
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstLiteralInstruction
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import app.revanced.util.transformMethods
import app.revanced.util.traverseClassHierarchy
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val EXTENSION_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR =
    "$SWIPE_PATH/SwipeControlsPatch;"

@Suppress("unused")
val swipeControlsPatch = bytecodePatch(
    SWIPE_CONTROLS.title,
    SWIPE_CONTROLS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        lockModeStateHookPatch,
        mainActivityResolvePatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {

        // region patch for swipe controls patch

        val hostActivityClass = swipeControlsHostActivityFingerprint.mutableClassOrThrow()
        val mainActivityClass = mainActivityMutableClass

        // inject the wrapper class from extension into the class hierarchy of MainActivity (WatchWhileActivity)
        hostActivityClass.setSuperClass(mainActivityClass.superclass)
        mainActivityClass.setSuperClass(hostActivityClass.type)

        // ensure all classes and methods in the hierarchy are non-final, so we can override them in extension
        traverseClassHierarchy(mainActivityClass) {
            accessFlags = accessFlags and AccessFlags.FINAL.value.inv()
            transformMethods {
                ImmutableMethod(
                    definingClass,
                    name,
                    parameters,
                    returnType,
                    accessFlags and AccessFlags.FINAL.value.inv(),
                    annotations,
                    hiddenApiRestrictions,
                    implementation
                ).toMutable()
            }
        }

        fullScreenEngagementOverlayFingerprint.methodOrThrow().apply {
            val viewIndex =
                indexOfFirstLiteralInstructionOrThrow(fullScreenEngagementOverlay) + 3
            val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA

            addInstruction(
                viewIndex + 1,
                "invoke-static {v$viewRegister}, $EXTENSION_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->setFullscreenEngagementOverlayView(Landroid/view/View;)V"
            )
        }

        // endregion

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: SWIPE_CONTROLS",
            "SETTINGS: DISABLE_WATCH_PANEL_GESTURES"
        )

        // region patch for disable HDR auto brightness

        // Since it does not support all versions,
        // add settings only if the patch is successful.
        if (!is_19_09_or_greater) {
            hdrBrightnessFingerprint.methodOrThrow().apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $EXTENSION_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->disableHDRAutoBrightness()Z
                        move-result v0
                        if-eqz v0, :default
                        return-void
                        """, ExternalLabel("default", getInstruction(0))
                )
                settingArray += "SETTINGS: DISABLE_HDR_BRIGHTNESS"
            }
        }

        // endregion

        // region patch for disable swipe to switch video

        if (is_19_23_or_greater) {
            swipeToSwitchVideoFingerprint.injectLiteralInstructionBooleanCall(
                SWIPE_TO_SWITCH_VIDEO_FEATURE_FLAG,
                "$EXTENSION_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->disableSwipeToSwitchVideo()Z"
            )

            settingArray += "SETTINGS: DISABLE_SWIPE_TO_SWITCH_VIDEO"
        }

        // endregion

        // region patch for disable watch panel gestures

        if (!is_19_36_or_greater) {
            watchPanelGesturesFingerprint.injectLiteralInstructionBooleanCall(
                WATCH_PANEL_GESTURES_FEATURE_FLAG,
                "$EXTENSION_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->disableWatchPanelGestures()Z"
            )
        } else {
            watchPanelGesturesAlternativeFingerprint.methodOrThrow().apply {
                val literalIndex = indexOfFirstLiteralInstruction(autoNavScrollCancelPadding)
                val middleIndex = indexOfFirstInstructionOrThrow(literalIndex) {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.returnType == "V" &&
                            reference.parameterTypes.size == 0
                }
                val targetIndex = indexOfFirstInstructionOrThrow(middleIndex + 1) {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.returnType == "V" &&
                            reference.parameterTypes.size == 0
                }
                if (getInstruction(targetIndex - 1).opcode != Opcode.IGET_OBJECT) {
                    throw PatchException("Previous Opcode pattern does not match: ${getInstruction(targetIndex - 1).opcode}")
                }
                if (getInstruction(targetIndex + 1).opcode != Opcode.IF_EQZ) {
                    throw PatchException("Next Opcode pattern does not match: ${getInstruction(targetIndex + 1).opcode}")
                }
                val fieldReference = getInstruction<ReferenceInstruction>(targetIndex - 1).reference
                val fieldInstruction = getInstruction<TwoRegisterInstruction>(targetIndex - 1)

                addInstructionsWithLabels(
                    targetIndex, """
                        invoke-static {}, $EXTENSION_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->disableWatchPanelGestures()Z
                        move-result v${fieldInstruction.registerA}
                        if-eqz v${fieldInstruction.registerA}, :disable
                        iget-object v${fieldInstruction.registerA}, v${fieldInstruction.registerB}, $fieldReference
                        """, ExternalLabel("disable", getInstruction(targetIndex + 1))
                )
                removeInstruction(targetIndex - 1)
            }
        }

        // endregion

        // region copy resources

        getContext().copyResources(
            "youtube/swipecontrols",
            ResourceGroup(
                "drawable",
                "ic_sc_brightness_auto.xml",
                "ic_sc_brightness_manual.xml",
                "ic_sc_volume_mute.xml",
                "ic_sc_volume_normal.xml"
            )
        )

        // endregion

        // region add settings

        addPreference(settingArray, SWIPE_CONTROLS)

        // endregion

    }
}
