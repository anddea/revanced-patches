package app.revanced.patches.youtube.player.seekbar

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.drawable.DrawableColorPatch
import app.revanced.patches.youtube.player.seekbar.fingerprints.CairoSeekbarConfigFingerprint
import app.revanced.patches.youtube.player.seekbar.fingerprints.ControlsOverlayStyleFingerprint
import app.revanced.patches.youtube.player.seekbar.fingerprints.SeekbarTappingFingerprint
import app.revanced.patches.youtube.player.seekbar.fingerprints.SeekbarThumbnailsQualityFingerprint
import app.revanced.patches.youtube.player.seekbar.fingerprints.ShortsSeekbarColorFingerprint
import app.revanced.patches.youtube.player.seekbar.fingerprints.ThumbnailPreviewConfigFingerprint
import app.revanced.patches.youtube.player.seekbar.fingerprints.TimeCounterFingerprint
import app.revanced.patches.youtube.player.seekbar.fingerprints.TimelineMarkerArrayFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.fingerprints.PlayerButtonsResourcesFingerprint
import app.revanced.patches.youtube.utils.fingerprints.PlayerButtonsVisibilityFingerprint
import app.revanced.patches.youtube.utils.fingerprints.PlayerSeekbarColorFingerprint
import app.revanced.patches.youtube.utils.fingerprints.SeekbarFingerprint
import app.revanced.patches.youtube.utils.fingerprints.SeekbarOnDrawFingerprint
import app.revanced.patches.youtube.utils.fingerprints.TotalTimeFingerprint
import app.revanced.patches.youtube.utils.flyoutmenu.FlyoutMenuHookPatch
import app.revanced.patches.youtube.utils.integrations.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.InlineTimeBarColorizedBarPlayedColorDark
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.InlineTimeBarPlayedNotHighlightedColor
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelTimeBarPlayedColor
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch.contexts
import app.revanced.patches.youtube.video.information.VideoInformationPatch
import app.revanced.util.*
import app.revanced.util.patch.BaseBytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

@Suppress("DEPRECATION", "unused")
object SeekbarComponentsPatch : BaseBytecodePatch(
    name = "Seekbar components",
    description = "Adds options to hide or change components related to the seekbar.",
    dependencies = setOf(
        DrawableColorPatch::class,
        FlyoutMenuHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        VideoInformationPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        CairoSeekbarConfigFingerprint,
        ControlsOverlayStyleFingerprint,
        PlayerButtonsResourcesFingerprint,
        PlayerSeekbarColorFingerprint,
        PlayerSeekbarColorFingerprint,
        SeekbarFingerprint,
        SeekbarTappingFingerprint,
        SeekbarThumbnailsQualityFingerprint,
        ShortsSeekbarColorFingerprint,
        TimelineMarkerArrayFingerprint,
        ThumbnailPreviewConfigFingerprint,
        TotalTimeFingerprint,
    )
) {
    private val CairoStartColor by stringPatchOption(
        key = "CairoStartColor",
        default = "#ffff2791",
        title = "Cairo start color",
        description = "Set Cairo start color for the seekbar."
    )

    private val CairoEndColor by stringPatchOption(
        key = "CairoEndColor",
        default = "#ffff0033",
        title = "Cairo end color",
        description = "Set Cairo end color for the seekbar."
    )

    override fun execute(context: BytecodeContext) {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: PLAYER",
            "SETTINGS: SEEKBAR_COMPONENTS"
        )

        // region patch for enable seekbar tapping patch

        SeekbarTappingFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val tapSeekIndex = it.scanResult.patternScanResult!!.startIndex + 1
                val tapSeekClass = getInstruction(tapSeekIndex)
                    .getReference<MethodReference>()!!
                    .definingClass

                val tapSeekMethods = context.findMethodsOrThrow(tapSeekClass)
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

                val insertIndex = it.scanResult.patternScanResult!!.startIndex + 2

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

        TotalTimeFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
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
        }

        // endregion

        // region patch for seekbar color

        PlayerSeekbarColorFingerprint.resultOrThrow().mutableMethod.apply {
            hook(InlineTimeBarColorizedBarPlayedColorDark)
            hook(InlineTimeBarPlayedNotHighlightedColor)
        }

        ShortsSeekbarColorFingerprint.resultOrThrow().mutableMethod.apply {
            hook(ReelTimeBarPlayedColor)
        }

        ControlsOverlayStyleFingerprint.resultOrThrow().let {
            val walkerMethod =
                it.getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex + 1)
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

        DrawableColorPatch.injectCall("$PLAYER_CLASS_DESCRIPTOR->getColor(I)I")

        contexts.xmlEditor["res/drawable/resume_playback_progressbar_drawable.xml"].use {
            val layerList = it.file.getElementsByTagName("layer-list").item(0) as Element
            val progressNode = layerList.getElementsByTagName("item").item(1) as Element
            if (!progressNode.getAttributeNode("android:id").value.endsWith("progress")) {
                throw PatchException("Could not find progress bar")
            }
            val scaleNode = progressNode.getElementsByTagName("scale").item(0) as Element
            val shapeNode = scaleNode.getElementsByTagName("shape").item(0) as Element
            val replacementNode = it.file.createElement(
                "app.revanced.integrations.youtube.patches.utils.ProgressBarDrawable"
            )
            scaleNode.replaceChild(replacementNode, shapeNode)
        }

        contexts.xmlEditor["res/values/colors.xml"].use { editor ->
            editor.file.doRecursively loop@{ node ->
                if (node is Element && node.tagName == "color") {
                    if (node.getAttribute("name") == "yt_youtube_magenta") {
                        node.textContent = CairoStartColor
                    }
                    if (node.getAttribute("name") == "yt_youtube_red_cairo") {
                        node.textContent = CairoEndColor
                    }
                }
            }
        }

        // endregion

        // region patch for high quality thumbnails

        // TODO: This will be added when support for newer YouTube versions is added.
        // SeekbarThumbnailsQualityFingerprint.injectLiteralInstructionBooleanCall(
        //     45399684,
        //     "$PLAYER_CLASS_DESCRIPTOR->enableHighQualityFullscreenThumbnails()Z"
        // )

        // endregion

        // region patch for hide chapter

        TimelineMarkerArrayFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
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
        }

        PlayerButtonsVisibilityFingerprint.alsoResolve(
            context, PlayerButtonsResourcesFingerprint
        ).let {
            it.mutableMethod.apply {
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
        }

        // endregion

        // region patch for hide seekbar

        SeekbarFingerprint.resultOrThrow().mutableClass.let { mutableClass ->
            SeekbarOnDrawFingerprint.also { it.resolve(context, mutableClass) }.resultOrThrow()
                .let {
                    it.mutableMethod.apply {
                        addInstructionsWithLabels(
                            0, """
                            invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideSeekbar()Z
                            move-result v0
                            if-eqz v0, :show
                            return-void
                            """, ExternalLabel("show", getInstruction(0))
                        )
                    }
                }
        }

        // endregion

        // region patch for hide time stamp

        PlayerSeekbarColorFingerprint.resultOrThrow().let { parentResult ->
            TimeCounterFingerprint.also { it.resolve(context, parentResult.classDef) }
                .resultOrThrow().let {
                    it.mutableMethod.apply {
                        addInstructionsWithLabels(
                            0, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideTimeStamp()Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(0))
                        )
                    }
                }
        }

        // endregion

        // region patch for restore old seekbar thumbnails

        ThumbnailPreviewConfigFingerprint.result?.let {
            ThumbnailPreviewConfigFingerprint.injectLiteralInstructionBooleanCall(
                45398577,
                "$PLAYER_CLASS_DESCRIPTOR->restoreOldSeekbarThumbnails()Z"
            )

            settingArray += "SETTINGS: RESTORE_OLD_SEEKBAR_THUMBNAILS"

            context.updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "OldSeekbarThumbnailsDefaultBoolean")
        }
            ?: println("WARNING: Restore old seekbar thumbnails setting is not supported in this version. Use YouTube 19.16.39 or earlier.")

        // endregion

        // region patch for enable cairo seekbar

        if (SettingsPatch.upward1923) {
            CairoSeekbarConfigFingerprint.injectLiteralInstructionBooleanCall(
                45617850,
                "$PLAYER_CLASS_DESCRIPTOR->enableCairoSeekbar()Z"
            )

            settingArray += "SETTINGS: ENABLE_CAIRO_SEEKBAR"
        }

        // endregion

        /**
         * Add settings
         */
        SettingsPatch.addPreference(settingArray)

        SettingsPatch.updatePatchStatus(this)
    }

    private fun MutableMethod.hook(literal: Long) {
        val insertIndex = indexOfFirstWideLiteralInstructionValueOrThrow(literal) + 2
        val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

        addInstructions(
            insertIndex + 1, """
                invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->overrideSeekbarColor(I)I
                move-result v$insertRegister
                """
        )
    }
}