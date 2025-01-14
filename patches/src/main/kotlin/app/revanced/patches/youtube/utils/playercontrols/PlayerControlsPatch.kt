package app.revanced.patches.youtube.utils.playercontrols

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.fullscreen.enterFullscreenMethod
import app.revanced.patches.youtube.utils.fullscreen.fullscreenButtonHookPatch
import app.revanced.patches.youtube.utils.playerButtonsResourcesFingerprint
import app.revanced.patches.youtube.utils.playerButtonsVisibilityFingerprint
import app.revanced.patches.youtube.utils.playservice.is_19_23_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_25_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.youtubeControlsOverlayFingerprint
import app.revanced.util.copyXmlNode
import app.revanced.util.findElementByAttributeValueOrThrow
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.inputStreamFromBundledResourceOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_PLAYER_CONTROLS_CLASS_DESCRIPTOR =
    "$UTILS_PATH/PlayerControlsPatch;"

private const val EXTENSION_PLAYER_CONTROLS_VISIBILITY_HOOK_CLASS_DESCRIPTOR =
    "$UTILS_PATH/PlayerControlsVisibilityHookPatch;"

lateinit var changeVisibilityMethod: MutableMethod
lateinit var changeVisibilityNegatedImmediatelyMethod: MutableMethod
lateinit var initializeBottomControlButtonMethod: MutableMethod
lateinit var initializeTopControlButtonMethod: MutableMethod

private val playerControlsBytecodePatch = bytecodePatch(
    description = "playerControlsBytecodePatch"
) {
    dependsOn(
        sharedExtensionPatch,
        sharedResourceIdPatch,
        versionCheckPatch,
        fullscreenButtonHookPatch,
    )

    execute {

        // region patch for hook player controls visibility

        playerControlsVisibilityEntityModelFingerprint.matchOrThrow().let {
            it.method.apply {
                val startIndex = it.patternMatch!!.startIndex
                val iGetReference = getInstruction<ReferenceInstruction>(startIndex).reference
                val staticReference = getInstruction<ReferenceInstruction>(startIndex + 1).reference

                it.classDef.methods.find { method -> method.name == "<init>" }?.apply {
                    val targetIndex = indexOfFirstInstructionOrThrow(Opcode.IPUT_OBJECT)
                    val targetRegister =
                        getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            iget v$targetRegister, v$targetRegister, $iGetReference
                            invoke-static {v$targetRegister}, $staticReference
                            move-result-object v$targetRegister
                            invoke-static {v$targetRegister}, $EXTENSION_PLAYER_CONTROLS_VISIBILITY_HOOK_CLASS_DESCRIPTOR->setPlayerControlsVisibility(Ljava/lang/Enum;)V
                            """
                    )
                } ?: throw PatchException("Constructor method not found")
            }
        }

        // endregion

        // region patch for hook visibility of play control buttons (e.g. pause, play button, etc)

        playerButtonsVisibilityFingerprint.methodOrThrow(playerButtonsResourcesFingerprint).apply {
            val viewIndex = indexOfFirstInstructionOrThrow(Opcode.INVOKE_INTERFACE)
            val viewRegister = getInstruction<FiveRegisterInstruction>(viewIndex).registerD

            addInstruction(
                viewIndex + 1,
                "invoke-static {p1, p2, v$viewRegister}, $EXTENSION_PLAYER_CONTROLS_CLASS_DESCRIPTOR->changeVisibility(ZZLandroid/view/View;)V"
            )
        }

        // endregion

        // region patch for hook visibility of play controls layout

        playerControlsVisibilityFingerprint.methodOrThrow(youtubeControlsOverlayFingerprint)
            .addInstruction(
                0,
                "invoke-static {p1}, $EXTENSION_PLAYER_CONTROLS_CLASS_DESCRIPTOR->changeVisibility(Z)V"
            )

        // endregion

        // region patch for detecting motion events in play controls layout

        motionEventFingerprint.methodOrThrow(youtubeControlsOverlayFingerprint).apply {
            val insertIndex = indexOfTranslationInstruction(this) + 1

            addInstruction(
                insertIndex,
                "invoke-static {}, $EXTENSION_PLAYER_CONTROLS_CLASS_DESCRIPTOR->changeVisibilityNegatedImmediate()V"
            )
        }

        // endregion

        // region patch for fix buttons do not hide immediately when fullscreen button is clicked

        // Reproduced only in RVX
        if (is_19_23_or_greater) {
            enterFullscreenMethod.addInstruction(
                0,
                "invoke-static {}, $EXTENSION_PLAYER_CONTROLS_CLASS_DESCRIPTOR->changeVisibilityNegatedImmediately()V"
            )
        }

        // endregion

        // region patch initialize of overlay button or SponsorBlock button

        mapOf(
            bottomControlsInflateFingerprint to "initializeBottomControlButton",
            controlsLayoutInflateFingerprint to "initializeTopControlButton"
        ).forEach { (fingerprint, methodName) ->
            fingerprint.matchOrThrow().let {
                it.method.apply {
                    val endIndex = it.patternMatch!!.endIndex
                    val viewRegister = getInstruction<OneRegisterInstruction>(endIndex).registerA

                    addInstruction(
                        endIndex + 1,
                        "invoke-static {v$viewRegister}, $EXTENSION_PLAYER_CONTROLS_CLASS_DESCRIPTOR->$methodName(Landroid/view/View;)V"
                    )
                }
            }
        }

        // endregion

        // region set methods to inject into extension

        changeVisibilityMethod =
            findMethodOrThrow(EXTENSION_PLAYER_CONTROLS_CLASS_DESCRIPTOR) {
                name == "changeVisibility" &&
                        parameters == listOf("Z", "Z")
            }

        changeVisibilityNegatedImmediatelyMethod =
            findMethodOrThrow(EXTENSION_PLAYER_CONTROLS_CLASS_DESCRIPTOR) {
                name == "changeVisibilityNegatedImmediately"
            }

        initializeBottomControlButtonMethod =
            findMethodOrThrow(EXTENSION_PLAYER_CONTROLS_CLASS_DESCRIPTOR) {
                name == "initializeBottomControlButton"
            }

        initializeTopControlButtonMethod =
            findMethodOrThrow(EXTENSION_PLAYER_CONTROLS_CLASS_DESCRIPTOR) {
                name == "initializeTopControlButton"
            }

        // endregion

        if (is_19_25_or_greater) {
            playerTopControlsExperimentalLayoutFeatureFlagFingerprint.methodOrThrow().apply {
                val index = indexOfFirstInstructionOrThrow(Opcode.MOVE_RESULT_OBJECT)
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1,
                    """
                        invoke-static { v$register }, $EXTENSION_PLAYER_CONTROLS_CLASS_DESCRIPTOR->getPlayerTopControlsLayoutResourceName(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                        """,
                )
            }
        }
    }
}

private fun MutableMethod.initializeHook(classDescriptor: String) =
    addInstruction(
        0,
        "invoke-static {p0}, $classDescriptor->initialize(Landroid/view/View;)V"
    )

private fun changeVisibilityHook(classDescriptor: String) =
    changeVisibilityMethod.addInstruction(
        0,
        "invoke-static {p0, p1}, $classDescriptor->changeVisibility(ZZ)V"
    )

private fun changeVisibilityNegatedImmediateHook(classDescriptor: String) =
    changeVisibilityNegatedImmediatelyMethod.addInstruction(
        0,
        "invoke-static {}, $classDescriptor->changeVisibilityNegatedImmediate()V"
    )

fun hookBottomControlButton(classDescriptor: String) {
    initializeBottomControlButtonMethod.initializeHook(classDescriptor)
    changeVisibilityHook(classDescriptor)
    changeVisibilityNegatedImmediateHook(classDescriptor)
}

fun hookTopControlButton(classDescriptor: String) {
    initializeTopControlButtonMethod.initializeHook(classDescriptor)
    changeVisibilityHook(classDescriptor)
    changeVisibilityNegatedImmediateHook(classDescriptor)
}

/**
 * Add a new top to the bottom of the YouTube player.
 *
 * @param resourceDirectoryName The name of the directory containing the hosting resource.
 */
@Suppress("KDocUnresolvedReference")
// Internal until this is modified to work with any patch (and not just SponsorBlock).
internal lateinit var addTopControl: (String) -> Unit
    private set

val playerControlsPatch = resourcePatch(
    description = "playerControlsPatch"
) {
    dependsOn(playerControlsBytecodePatch)

    execute {
        addTopControl = { resourceDirectoryName ->
            val resourceFileName = "shared/host/layout/youtube_controls_layout.xml"
            val hostingResourceStream = inputStreamFromBundledResourceOrThrow(
                resourceDirectoryName,
                resourceFileName,
            )

            val document = document("res/layout/youtube_controls_layout.xml")

            "RelativeLayout".copyXmlNode(
                document(hostingResourceStream),
                document,
            ).use {
                val element = document.childNodes.findElementByAttributeValueOrThrow(
                    "android:id",
                    "@id/player_video_heading",
                )

                // FIXME: This uses hard coded values that only works with SponsorBlock.
                // If other top buttons are added by other patches, this code must be changed.
                // voting button id from the voting button view from the youtube_controls_layout.xml host file
                val votingButtonId = "@+id/revanced_sb_voting_button"
                element.attributes.getNamedItem("android:layout_toStartOf").nodeValue =
                    votingButtonId
            }
        }
    }
}
