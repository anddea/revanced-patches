package app.revanced.patches.youtube.player.seekbar

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.drawable.addDrawableColorHook
import app.revanced.patches.shared.drawable.drawableColorHookPatch
import app.revanced.patches.shared.mainactivity.onCreateMethod
import app.revanced.patches.youtube.layout.branding.icon.customBrandingIconPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.revanced.patches.youtube.utils.flyoutmenu.flyoutMenuHookPatch
import app.revanced.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import app.revanced.patches.youtube.utils.patch.PatchList.CUSTOM_BRANDING_ICON_FOR_YOUTUBE
import app.revanced.patches.youtube.utils.patch.PatchList.SEEKBAR_COMPONENTS
import app.revanced.patches.youtube.utils.playerButtonsResourcesFingerprint
import app.revanced.patches.youtube.utils.playerButtonsVisibilityFingerprint
import app.revanced.patches.youtube.utils.playerSeekbarColorFingerprint
import app.revanced.patches.youtube.utils.playservice.is_19_23_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_25_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_46_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_49_or_greater
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
import app.revanced.util.Utils.printWarn
import app.revanced.util.findElementByAttributeValueOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.resolvable
import app.revanced.util.inputStreamFromBundledResource
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element
import java.io.ByteArrayInputStream

internal const val splashSeekbarColorAttributeName = "splash_custom_seekbar_color"

/**
 * Generate a style xml with all combinations of 9-bit colors.
 */
private fun create9BitSeekbarColorStyles(): String = StringBuilder().apply {
    append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
    append("<resources>\n")

    for (red in 0..7) {
        for (green in 0..7) {
            for (blue in 0..7) {
                val name = "${red}_${green}_${blue}"

                fun roundTo3BitHex(channel8Bits: Int) =
                    (channel8Bits * 255 / 7).toString(16).padStart(2, '0')

                val r = roundTo3BitHex(red)
                val g = roundTo3BitHex(green)
                val b = roundTo3BitHex(blue)
                val color = "#ff$r$g$b"

                append(
                    """
                        <style name="splash_seekbar_color_style_$name">
                            <item name="$splashSeekbarColorAttributeName">$color</item>
                        </style>
                    """
                )
            }
        }
    }

    append("</resources>")
}.toString()

private const val EXTENSION_SEEKBAR_COLOR_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/SeekbarColorPatch;"

@Suppress("unused")
val seekbarComponentsPatch = bytecodePatch(
    SEEKBAR_COMPONENTS.title,
    SEEKBAR_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        drawableColorHookPatch,
        flyoutMenuHookPatch,
        mainActivityResolvePatch,
        sharedResourceIdPatch,
        settingsPatch,
        videoInformationPatch,
        versionCheckPatch,
    )

    execute {

        val restoreOldSplashAnimationIncluded = CUSTOM_BRANDING_ICON_FOR_YOUTUBE.included == true &&
                customBrandingIconPatch.getBooleanOptionValue("restoreOldSplashAnimationOption").value == true

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

        // region patch for enable cairo seekbar

        if (is_19_23_or_greater) {
            cairoSeekbarConfigFingerprint.injectLiteralInstructionBooleanCall(
                45617850L,
                "$PLAYER_CLASS_DESCRIPTOR->enableCairoSeekbar()Z"
            )

            settingArray += "SETTINGS: ENABLE_CAIRO_SEEKBAR"
        }

        // endregion

        // region patch for gradient seekbar color and bounds

        if (is_19_25_or_greater) {
            // In version 19.23, gradient colors and bounds use a different structure,
            // and the implementation is still raw/underdeveloped.

            // Adjust gradient seekbar colors
            playerLinearGradientLegacyFingerprint.matchOrThrow().let {
                it.method.apply {
                    val index = it.patternMatch!!.endIndex
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructions(
                        index + 1,
                        """
                            invoke-static { v$register },  $EXTENSION_SEEKBAR_COLOR_CLASS_DESCRIPTOR->setSeekbarGradientColors([I)[I
                            move-result-object v$register
                            """
                    )
                }
            }

            // Adjust gradient seekbar positions
            setBoundsFingerprint.methodOrThrow().apply {
                val newArrayIndex = indexOfFirstInstructionOrThrow(Opcode.NEW_ARRAY)
                val arrayRegister = getInstruction<OneRegisterInstruction>(newArrayIndex).registerA

                val smaliInstruction = """
                    invoke-static/range { v$arrayRegister }, $EXTENSION_SEEKBAR_COLOR_CLASS_DESCRIPTOR->setSeekbarGradientPositions([F)V
                """.trimIndent()

                addInstruction(
                    indexOfFirstInstructionOrThrow(Opcode.FILL_ARRAY_DATA) + 1,
                    smaliInstruction
                )
            }

            // Set seekbar thumb color
            seekbarThumbFingerprint.methodOrThrow().apply {
                val instructions = implementation!!.instructions.toList()

                val lastMoveResultIndex = instructions.indexOfLast { it.opcode == Opcode.MOVE_RESULT }

                if (lastMoveResultIndex == -1) {
                    throw PatchException("Could not find the last move-result instruction")
                }

                val resultRegister = (instructions[lastMoveResultIndex] as? OneRegisterInstruction)?.registerA
                    ?: throw PatchException("Could not get the register used in the last move-result instruction")

                val smaliInstruction = """
                    invoke-static {}, $EXTENSION_SEEKBAR_COLOR_CLASS_DESCRIPTOR->setSeekbarThumbColor()I
                    move-result v$resultRegister
                """.trimIndent()

                addInstructions(lastMoveResultIndex + 1, smaliInstruction)
            }

            settingArray += "SETTINGS: GRADIENT_SEEKBAR_OPTIONS"
        }

        // endregion patch for gradient seekbar color and bounds

        // region patch for seekbar color

        fun MutableMethod.addColorChangeInstructions(literal: Long) {
            val insertIndex = indexOfFirstLiteralInstructionOrThrow(literal) + 2
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex + 1, """
                    invoke-static {v$insertRegister}, $EXTENSION_SEEKBAR_COLOR_CLASS_DESCRIPTOR->getVideoPlayerSeekbarColor(I)I
                    move-result v$insertRegister
                    """
            )
        }

        playerSeekbarColorFingerprint.methodOrThrow().apply {
            addColorChangeInstructions(inlineTimeBarColorizedBarPlayedColorDark)
            addColorChangeInstructions(inlineTimeBarPlayedNotHighlightedColor)
        }

        shortsSeekbarColorFingerprint.methodOrThrow().apply {
            addColorChangeInstructions(reelTimeBarPlayedColor)
        }

        controlsOverlayStyleFingerprint.matchOrThrow().let {
            val walkerMethod =
                it.getWalkerMethod(it.patternMatch!!.startIndex + 1)
            walkerMethod.apply {
                val colorRegister = getInstruction<TwoRegisterInstruction>(0).registerA

                addInstructions(
                    0, """
                        invoke-static {v$colorRegister}, $EXTENSION_SEEKBAR_COLOR_CLASS_DESCRIPTOR->getVideoPlayerSeekbarClickedColor(I)I
                        move-result v$colorRegister
                        """
                )
            }
        }

        addDrawableColorHook("$EXTENSION_SEEKBAR_COLOR_CLASS_DESCRIPTOR->getLithoColor(I)I")

        if (is_19_25_or_greater) {
            lithoLinearGradientFingerprint.methodOrThrow().addInstruction(
                0,
                "invoke-static/range { p4 .. p5 },  $EXTENSION_SEEKBAR_COLOR_CLASS_DESCRIPTOR->setLinearGradient([I[F)V"
            )

            if (!is_19_49_or_greater) {
                playerLinearGradientLegacyFingerprint.matchOrThrow().let {
                    it.method.apply {
                        val index = it.patternMatch!!.endIndex
                        val register = getInstruction<OneRegisterInstruction>(index).registerA

                        addInstructions(
                            index + 1,
                            """
                            invoke-static { v$register },  $EXTENSION_SEEKBAR_COLOR_CLASS_DESCRIPTOR->getLinearGradient([I)[I
                            move-result-object v$register
                            """
                        )
                    }
                }
            } else {
                // TODO: add 19.49 support
                playerSeekbarGradientConfigFingerprint.injectLiteralInstructionBooleanCall(
                    PLAYER_SEEKBAR_GRADIENT_FEATURE_FLAG,
                    "$EXTENSION_SEEKBAR_COLOR_CLASS_DESCRIPTOR->playerSeekbarGradientEnabled(Z)Z"
                )
            }


            if (!restoreOldSplashAnimationIncluded) {
                // Don't use the lotte splash screen layout if using custom seekbar.
                arrayOf(
                    launchScreenLayoutTypeFingerprint.methodOrThrow(),
                    onCreateMethod
                ).forEach { method ->
                    method.apply {
                        val literalIndex =
                            indexOfFirstLiteralInstructionOrThrow(
                                launchScreenLayoutTypeLotteFeatureFlag
                            )
                        val resultIndex =
                            indexOfFirstInstructionOrThrow(literalIndex, Opcode.MOVE_RESULT)
                        val register = getInstruction<OneRegisterInstruction>(resultIndex).registerA

                        addInstructions(
                            resultIndex + 1,
                            """
                            invoke-static { v$register }, $EXTENSION_SEEKBAR_COLOR_CLASS_DESCRIPTOR->useLotteLaunchSplashScreen(Z)Z
                            move-result v$register
                            """
                        )
                    }
                }
            }

            // Hook the splash animation drawable to set the seekbar color theme.
            onCreateMethod.apply {
                val drawableIndex = indexOfFirstInstructionOrThrow {
                    val reference = getReference<MethodReference>()
                    reference?.definingClass == "Landroid/widget/ImageView;" &&
                            reference.name == "getDrawable"
                }
                val checkCastIndex =
                    indexOfFirstInstructionOrThrow(drawableIndex, Opcode.CHECK_CAST)
                val drawableRegister =
                    getInstruction<OneRegisterInstruction>(checkCastIndex).registerA

                addInstruction(
                    checkCastIndex + 1,
                    "invoke-static { v$drawableRegister }, $EXTENSION_SEEKBAR_COLOR_CLASS_DESCRIPTOR->" +
                            "setSplashAnimationDrawableTheme(Landroid/graphics/drawable/AnimatedVectorDrawable;)V"
                )
            }
        }

        val context = getContext()

        context.document("res/drawable/resume_playback_progressbar_drawable.xml")
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

        if (is_19_25_or_greater && !restoreOldSplashAnimationIncluded) {
            // Add attribute and styles for splash screen custom color.
            // Using a style is the only way to selectively change just the seekbar fill color.
            //
            // Because the style colors must be hard coded for all color possibilities,
            // instead of allowing 24 bit color the style is restricted to 9-bit (3 bits per color channel)
            // and the style color closest to the users custom color is used for the splash screen.
            arrayOf(
                inputStreamFromBundledResource(
                    "youtube/seekbar/values",
                    "attrs.xml"
                )!! to "res/values/attrs.xml",
                ByteArrayInputStream(create9BitSeekbarColorStyles().toByteArray()) to "res/values/styles.xml"
            ).forEach { (source, destination) ->
                "resources".copyXmlNode(
                    context.document(source),
                    context.document(destination),
                ).close()
            }

            fun setSplashDrawablePathFillColor(
                xmlFileNames: Iterable<String>,
                vararg resourceNames: String
            ) {
                xmlFileNames.forEach { xmlFileName ->
                    context.document(xmlFileName).use { document ->
                        resourceNames.forEach { elementId ->
                            val element = document.childNodes.findElementByAttributeValueOrThrow(
                                "android:name",
                                elementId
                            )

                            val attribute = "android:fillColor"
                            if (!element.hasAttribute(attribute)) {
                                throw PatchException("Could not find $attribute for $elementId")
                            }

                            element.setAttribute(
                                attribute,
                                "?attr/$splashSeekbarColorAttributeName"
                            )
                        }
                    }
                }
            }

            try {
                setSplashDrawablePathFillColor(
                    listOf(
                        "res/drawable/\$startup_animation_light__0.xml",
                        "res/drawable/\$startup_animation_dark__0.xml"
                    ),
                    "_R_G_L_10_G_D_0_P_0"
                )
            } catch (_: Exception) {
                // Do nothing
            }

            if (!is_19_46_or_greater) {
                // Resources removed in 19.46+
                setSplashDrawablePathFillColor(
                    listOf(
                        "res/drawable/\$buenos_aires_animation_light__0.xml",
                        "res/drawable/\$buenos_aires_animation_dark__0.xml"
                    ),
                    "_R_G_L_8_G_D_0_P_0"
                )
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
            printWarn("\"Restore old seekbar thumbnails\" is not supported in this version. Use YouTube 19.16.39 or earlier.")
        }

        // endregion

        // region add settings

        addPreference(settingArray, SEEKBAR_COMPONENTS)

        // endregion

    }
}
