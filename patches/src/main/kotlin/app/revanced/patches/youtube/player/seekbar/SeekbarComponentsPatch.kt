package app.revanced.patches.youtube.player.seekbar

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.drawable.addDrawableColorHook
import app.revanced.patches.shared.drawable.drawableColorHookPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.flyoutmenu.flyoutMenuHookPatch
import app.revanced.patches.youtube.utils.patch.PatchList.SEEKBAR_COMPONENTS
import app.revanced.patches.youtube.utils.playerButtonsResourcesFingerprint
import app.revanced.patches.youtube.utils.playerButtonsVisibilityFingerprint
import app.revanced.patches.youtube.utils.playerSeekbarColorFingerprint
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.resourceid.inlineTimeBarColorizedBarPlayedColorDark
import app.revanced.patches.youtube.utils.resourceid.inlineTimeBarPlayedNotHighlightedColor
import app.revanced.patches.youtube.utils.resourceid.reelTimeBarPlayedColor
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.seekbarFingerprint
import app.revanced.patches.youtube.utils.seekbarOnDrawFingerprint
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.ResourceUtils.getContext
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.utils.totalTimeFingerprint
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.util.*
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.resolvable
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

@Suppress("unused")
val seekbarComponentsPatch = bytecodePatch(
    SEEKBAR_COMPONENTS.title,
    SEEKBAR_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        drawableColorHookPatch,
        flyoutMenuHookPatch,
        sharedResourceIdPatch,
        settingsPatch,
        videoInformationPatch,
        versionCheckPatch,
    )

    val cairoStartColor by stringOption(
        key = "cairoStartColor",
        default = "#FFFF2791",
        title = "Cairo start color",
        description = "Set Cairo start color for the seekbar."
    )

    val cairoEndColor by stringOption(
        key = "cairoEndColor",
        default = "#FFFF0033",
        title = "Cairo end color",
        description = "Set Cairo end color for the seekbar."
    )

    execute {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: PLAYER",
            "SETTINGS: SEEKBAR_COMPONENTS"
        )

        // region patch for enable seekbar tapping patch

        seekbarTappingFingerprint.matchOrThrow().let {
            it.method.apply {
                val tapSeekIndex = it.patternMatch!!.startIndex + 1
                val tapSeekClass = getInstruction(tapSeekIndex)
                    .getReference<MethodReference>()!!
                    .definingClass

                val tapSeekMethods = findMethodsOrThrow(tapSeekClass)
                var pMethodCall = ""
                var oMethodCall = ""

                for (method in tapSeekMethods) {
                    if (method.implementation == null)
                        continue

                    val instructions = method.implementation!!.instructions
                    // here we make sure we actually find the method because it has more than 7 instructions
                    if (instructions.count() != 10)
                        continue

                    // we know that the 7th instruction has the opcode CONST_4
                    val instruction = instructions.elementAt(6)
                    if (instruction.opcode != Opcode.CONST_4)
                        continue

                    // the literal for this instruction has to be either 1 or 2
                    val literal = (instruction as NarrowLiteralInstruction).narrowLiteral

                    // method founds
                    if (literal == 1)
                        pMethodCall = "${method.definingClass}->${method.name}(I)V"
                    else if (literal == 2)
                        oMethodCall = "${method.definingClass}->${method.name}(I)V"
                }

                if (pMethodCall.isEmpty()) {
                    throw PatchException("pMethod not found")
                }
                if (oMethodCall.isEmpty()) {
                    throw PatchException("oMethod not found")
                }

                val insertIndex = it.patternMatch!!.startIndex + 2

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->enableSeekbarTapping()Z
                        move-result v0
                        if-eqz v0, :disabled
                        invoke-virtual { p0, v2 }, $pMethodCall
                        invoke-virtual { p0, v2 }, $oMethodCall
                        """, ExternalLabel("disabled", getInstruction(insertIndex))
                )
            }
        }

        // endregion

        // region patch for append time stamps information

        totalTimeFingerprint.methodOrThrow().apply {
            val charSequenceIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "getString"
            } + 1
            val charSequenceRegister =
                getInstruction<OneRegisterInstruction>(charSequenceIndex).registerA
            val textViewIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "getText"
            }
            val textViewRegister =
                getInstruction<FiveRegisterInstruction>(textViewIndex).registerC

            addInstructions(
                textViewIndex, """
                    invoke-static {v$textViewRegister}, $PLAYER_CLASS_DESCRIPTOR->setContainerClickListener(Landroid/view/View;)V
                    invoke-static {v$charSequenceRegister}, $PLAYER_CLASS_DESCRIPTOR->appendTimeStampInformation(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$charSequenceRegister
                    """
            )
        }

        // endregion

        // region patch for seekbar color

        fun MutableMethod.hookSeekbarColor(literal: Long) {
            val insertIndex = indexOfFirstLiteralInstructionOrThrow(literal) + 2
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex + 1, """
                    invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->overrideSeekbarColor(I)I
                    move-result v$insertRegister
                    """
            )
        }


        playerSeekbarColorFingerprint.methodOrThrow().apply {
            hookSeekbarColor(inlineTimeBarColorizedBarPlayedColorDark)
            hookSeekbarColor(inlineTimeBarPlayedNotHighlightedColor)
        }

        shortsSeekbarColorFingerprint.methodOrThrow().apply {
            hookSeekbarColor(reelTimeBarPlayedColor)
        }

        controlsOverlayStyleFingerprint.matchOrThrow().let {
            val walkerMethod =
                it.getWalkerMethod(it.patternMatch!!.startIndex + 1)
            walkerMethod.apply {
                val colorRegister = getInstruction<TwoRegisterInstruction>(0).registerA

                addInstructions(
                    0, """
                        invoke-static {v$colorRegister}, $PLAYER_CLASS_DESCRIPTOR->getSeekbarClickedColorValue(I)I
                        move-result v$colorRegister
                        """
                )
            }
        }

        addDrawableColorHook("$PLAYER_CLASS_DESCRIPTOR->getColor(I)I")

        getContext().document("res/drawable/resume_playback_progressbar_drawable.xml")
            .use { document ->
                val layerList = document.getElementsByTagName("layer-list").item(0) as Element
                val progressNode = layerList.getElementsByTagName("item").item(1) as Element
                if (!progressNode.getAttributeNode("android:id").value.endsWith("progress")) {
                    throw PatchException("Could not find progress bar")
                }
                val scaleNode = progressNode.getElementsByTagName("scale").item(0) as Element
                val shapeNode = scaleNode.getElementsByTagName("shape").item(0) as Element
                val replacementNode = document.createElement(
                    "app.revanced.extension.youtube.patches.utils.ProgressBarDrawable"
                )
                scaleNode.replaceChild(replacementNode, shapeNode)
            }

        getContext().document("res/values/colors.xml").use { document ->
            document.doRecursively loop@{ node ->
                if (node is Element && node.tagName == "color") {
                    if (node.getAttribute("name") == "yt_youtube_red_cairo") {
                        node.textContent = cairoStartColor
                    }
                    if (node.getAttribute("name") == "yt_youtube_magenta") {
                        node.textContent = cairoEndColor
                    }
                }
            }
        }

        // endregion

        // region patch for high quality thumbnails

        // TODO: This will be added when support for newer YouTube versions is added.
        // seekbarThumbnailsQualityFingerprint.injectLiteralInstructionBooleanCall(
        //     45399684L,
        //     "$PLAYER_CLASS_DESCRIPTOR->enableHighQualityFullscreenThumbnails()Z"
        // )

        // endregion

        // region patch for hide chapter

        timelineMarkerArrayFingerprint.methodOrThrow().apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->disableSeekbarChapters()Z
                    move-result v0
                    if-eqz v0, :show
                    const/4 v0, 0x0
                    new-array v0, v0, [Lcom/google/android/libraries/youtube/player/features/overlay/timebar/TimelineMarker;
                    return-object v0
                    """, ExternalLabel("show", getInstruction(0))
            )
        }

        playerButtonsVisibilityFingerprint.methodOrThrow(playerButtonsResourcesFingerprint).apply {
            val freeRegister = implementation!!.registerCount - parameters.size - 2
            val viewIndex = indexOfFirstInstructionOrThrow(Opcode.INVOKE_INTERFACE)
            val viewRegister = getInstruction<FiveRegisterInstruction>(viewIndex).registerD

            addInstructionsWithLabels(
                viewIndex, """
                    invoke-static {v$viewRegister}, $PLAYER_CLASS_DESCRIPTOR->hideSeekbarChapterLabel(Landroid/view/View;)Z
                    move-result v$freeRegister
                    if-eqz v$freeRegister, :ignore
                    return-void
                    """, ExternalLabel("ignore", getInstruction(viewIndex))
            )
        }

        // endregion

        // region patch for hide seekbar

        seekbarOnDrawFingerprint.methodOrThrow(seekbarFingerprint).apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideSeekbar()Z
                    move-result v0
                    if-eqz v0, :show
                    return-void
                    """, ExternalLabel("show", getInstruction(0))
            )
        }

        // endregion

        // region patch for hide time stamp

        timeCounterFingerprint.methodOrThrow(playerSeekbarColorFingerprint).apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideTimeStamp()Z
                    move-result v0
                    if-eqz v0, :show
                    return-void
                    """, ExternalLabel("show", getInstruction(0))
            )
        }

        // endregion

        // region patch for restore old seekbar thumbnails

        if (thumbnailPreviewConfigFingerprint.resolvable()) {
            thumbnailPreviewConfigFingerprint.injectLiteralInstructionBooleanCall(
                45398577L,
                "$PLAYER_CLASS_DESCRIPTOR->restoreOldSeekbarThumbnails()Z"
            )

            settingArray += "SETTINGS: RESTORE_OLD_SEEKBAR_THUMBNAILS"

            updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "OldSeekbarThumbnailsDefaultBoolean")
        } else {
            println("WARNING: Restore old seekbar thumbnails setting is not supported in this version. Use YouTube 19.16.39 or earlier.")
        }

        // endregion

        // region add settings

        addPreference(settingArray, SEEKBAR_COMPONENTS)

        // endregion

    }
}
