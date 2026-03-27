package app.morphe.patches.youtube.utils.sponsorblock

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.patch.PatchList.SPONSORBLOCK
import app.morphe.patches.youtube.utils.playercontrols.addTopControl
import app.morphe.patches.youtube.utils.playercontrols.injectControl
import app.morphe.patches.youtube.utils.playercontrols.playerControlsPatch
import app.morphe.patches.youtube.utils.resourceid.insetOverlayViewLayout
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.seekbarFingerprint
import app.morphe.patches.youtube.utils.seekbarOnDrawFingerprint
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.utils.totalTimeFingerprint
import app.morphe.patches.youtube.utils.youtubeControlsOverlayFingerprint
import app.morphe.patches.youtube.video.information.hookVideoInformation
import app.morphe.patches.youtube.video.information.onCreateHook
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.information.videoTimeHook
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.morphe.util.*
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val EXTENSION_SPONSOR_BLOCK_PATH =
    "$EXTENSION_PATH/sponsorblock"

private const val EXTENSION_SPONSOR_BLOCK_UI_PATH =
    "$EXTENSION_SPONSOR_BLOCK_PATH/ui"

private const val EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR =
    "$EXTENSION_SPONSOR_BLOCK_PATH/SegmentPlaybackController;"

private const val EXTENSION_SPONSOR_BLOCK_VIEW_CONTROLLER_CLASS_DESCRIPTOR =
    "$EXTENSION_SPONSOR_BLOCK_UI_PATH/SponsorBlockViewController;"

val sponsorBlockBytecodePatch = bytecodePatch(
    description = "sponsorBlockBytecodePatch"
) {
    dependsOn(
        sharedResourceIdPatch,
        videoInformationPatch,
    )

    execute {
        // Hook the video time method
        videoTimeHook(
            EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR,
            "setVideoTime"
        )
        // Initialize the player controller
        onCreateHook(
            EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR,
            "initialize"
        )


        seekbarOnDrawFingerprint.methodOrThrow(seekbarFingerprint).apply {
            // Get left and right of seekbar rectangle
            val moveObjectIndex = indexOfFirstInstructionOrThrow(Opcode.MOVE_OBJECT_FROM16)

            addInstruction(
                moveObjectIndex + 1,
                "invoke-static/range {p0 .. p0}, " +
                        "$EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;)V"
            )

            // Set seekbar thickness
            val roundIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "round"
            } + 1
            val roundRegister = getInstruction<OneRegisterInstruction>(roundIndex).registerA

            addInstruction(
                roundIndex + 1,
                "invoke-static {v$roundRegister}, " +
                        "$EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarThickness(I)V"
            )

            // Draw segment
            val drawCircleIndex = indexOfFirstInstructionReversedOrThrow {
                getReference<MethodReference>()?.name == "drawCircle"
            }
            val drawCircleInstruction = getInstruction<FiveRegisterInstruction>(drawCircleIndex)
            addInstruction(
                drawCircleIndex,
                "invoke-static {v${drawCircleInstruction.registerC}, v${drawCircleInstruction.registerE}}, " +
                        "$EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
            )
        }

        // Voting & Shield button
        setOf(
            "CreateSegmentButton",
            "VotingButton"
        ).forEach { className ->
            injectControl("$EXTENSION_SPONSOR_BLOCK_UI_PATH/${className};")
        }

        // Skip button
        injectControl(
            descriptor = EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR,
            topControl = false,
            initialize = false
        )

        // Append timestamp
        totalTimeFingerprint.methodOrThrow().apply {
            val targetIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "getString"
            } + 1
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstructions(
                targetIndex + 1, """
                        invoke-static {v$targetRegister}, $EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->appendTimeWithoutSegments(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
            )
        }

        // Initialize the SponsorBlock view
        youtubeControlsOverlayFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex =
                    indexOfFirstLiteralInstructionOrThrow(insetOverlayViewLayout)
                val checkCastIndex = indexOfFirstInstructionOrThrow(targetIndex, Opcode.CHECK_CAST)
                val targetRegister =
                    getInstruction<OneRegisterInstruction>(checkCastIndex).registerA

                addInstruction(
                    checkCastIndex + 1,
                    "invoke-static {v$targetRegister}, $EXTENSION_SPONSOR_BLOCK_VIEW_CONTROLLER_CLASS_DESCRIPTOR->initialize(Landroid/view/ViewGroup;)V"
                )
            }
        }

        // Replace strings
        rectangleFieldInvalidatorFingerprint.methodOrThrow(seekbarFingerprint).apply {
            val invalidateIndex = indexOfInvalidateInstruction(this)
            val rectangleIndex = indexOfFirstInstructionReversedOrThrow(invalidateIndex + 1) {
                getReference<FieldReference>()?.type == "Landroid/graphics/Rect;"
            }
            val rectangleFieldName =
                (getInstruction<ReferenceInstruction>(rectangleIndex).reference as FieldReference).name

            segmentPlaybackControllerFingerprint.matchOrThrow().let {
                it.method.apply {
                    val replaceIndex = it.instructionMatches.first().index
                    val replaceRegister =
                        getInstruction<OneRegisterInstruction>(replaceIndex).registerA

                    replaceInstruction(
                        replaceIndex,
                        "const-string v$replaceRegister, \"$rectangleFieldName\""
                    )
                }
            }
        }

        // Set current video id
        hookVideoInformation("$EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")

        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "SponsorBlock")
    }
}

private const val RIGHT = "right"

@Suppress("unused")
val sponsorBlockPatch = resourcePatch(
    SPONSORBLOCK.title,
    SPONSORBLOCK.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        playerControlsPatch,
        sponsorBlockBytecodePatch,
        settingsPatch,
    )

    val newSegmentAlignment by stringOption(
        key = "NewSegmentAlignment",
        default = RIGHT,
        values = mapOf(
            "Right" to RIGHT,
            "Left" to "left",
        ),
        title = "New segment alignment",
        description = "Align new segment window.",
        required = true
    )

    execute {
        /**
         * merge SponsorBlock drawables to main drawables
         */
        arrayOf(
            ResourceGroup(
                "layout",
                "revanced_sb_inline_sponsor_overlay.xml",
                "revanced_sb_skip_sponsor_button.xml"
            ),
            ResourceGroup(
                "drawable",
                "revanced_sb_drag_handle.xml",
                "revanced_sb_new_segment_background.xml",
                "revanced_sb_skip_sponsor_button_background.xml"
            )
        ).forEach { resourceGroup ->
            copyResources("youtube/sponsorblock/shared", resourceGroup)
        }

        arrayOf(
            ResourceGroup(
                "layout",
                "revanced_sb_new_segment.xml"
            ),
            ResourceGroup(
                "drawable",
                "revanced_sb_adjust.xml",
                "revanced_sb_backward.xml",
                "revanced_sb_compare.xml",
                "revanced_sb_edit.xml",
                "revanced_sb_forward.xml",
                "revanced_sb_logo.xml",
                "revanced_sb_publish.xml",
                "revanced_sb_voting.xml"
            )
        ).forEach { resourceGroup ->
            copyResources("youtube/sponsorblock/outline", resourceGroup)
        }

        if (newSegmentAlignment == "left") {
            document("res/layout/revanced_sb_inline_sponsor_overlay.xml").use { document ->
                document.doRecursively loop@{ node ->
                    if (node is Element && node.tagName == "app.morphe.extension.youtube.sponsorblock.ui.NewSegmentLayout") {
                        node.setAttribute("android:layout_alignParentRight", "false")
                        node.setAttribute("android:layout_alignParentLeft", "true")
                    }
                }
            }
        }

        /**
         * merge xml nodes from the host to their real xml files
         */
        addTopControl(
            "youtube/sponsorblock/shared",
            "@+id/revanced_sb_voting_button",
            "@+id/revanced_sb_create_segment_button"
        )

        /**
         * Add settings
         */
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: SPONSOR_BLOCK"
            ),
            SPONSORBLOCK
        )
    }
}
