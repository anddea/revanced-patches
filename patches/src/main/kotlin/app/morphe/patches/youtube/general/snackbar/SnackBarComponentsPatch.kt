package app.morphe.patches.youtube.general.snackbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.drawable.addDrawableColorHook
import app.morphe.patches.shared.drawable.drawableColorHookPatch
import app.morphe.patches.shared.spans.addSpanFilter
import app.morphe.patches.shared.spans.inclusiveSpanPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.extension.Constants.SPANS_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.SNACK_BAR_COMPONENTS
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getNode
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.valueOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/SnackBarPatch;"
private const val FILTER_CLASS_DESCRIPTOR =
    "$SPANS_PATH/SnackBarFilter;"

private val snackBarComponentsBytecodePatch = bytecodePatch(
    description = "snackBarComponentsBytecodePatch"
) {
    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
        drawableColorHookPatch,
        inclusiveSpanPatch,
    )

    execute {
        bottomUiContainerFingerprint.methodOrThrow().apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hideSnackBar()Z
                    move-result v0
                    if-eqz v0, :show
                    return-void
                    """, ExternalLabel("show", getInstruction(0))
            )
        }

        bottomUiContainerPreFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.instructionMatches.first().index + 1

                addInstruction(
                    insertIndex,
                    "invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->lithoSnackBarLoaded()V"
                )
            }
        }

        bottomUiContainerThemeFingerprint.matchOrThrow().let {
            it.method.apply {
                val darkThemeIndex = it.instructionMatches.first().index + 2
                val darkThemeReference =
                    getInstruction<ReferenceInstruction>(darkThemeIndex).reference.toString()

                implementation!!.instructions
                    .withIndex()
                    .filter { (_, instruction) ->
                        instruction.opcode == Opcode.SGET_OBJECT &&
                                (instruction as? ReferenceInstruction)?.reference?.toString() == darkThemeReference
                    }
                    .map { (index, _) -> index }
                    .reversed()
                    .forEach { index ->
                        val appThemeIndex =
                            indexOfFirstInstructionReversedOrThrow(index, Opcode.MOVE_RESULT_OBJECT)
                        val appThemeRegister =
                            getInstruction<OneRegisterInstruction>(appThemeIndex).registerA
                        val darkThemeRegister =
                            getInstruction<OneRegisterInstruction>(index).registerA

                        addInstructions(
                            index + 1, """
                                invoke-static {v$appThemeRegister, v$darkThemeRegister}, $EXTENSION_CLASS_DESCRIPTOR->invertSnackBarTheme(Ljava/lang/Enum;Ljava/lang/Enum;)Ljava/lang/Enum;
                                move-result-object v$appThemeRegister
                                """
                        )
                    }
            }
        }

        fun MutableMethod.setBackground(index: Int, register: Int) =
            addInstruction(
                index,
                "invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->setLithoSnackBarBackground(Landroid/view/View;)V"
            )

        lithoSnackBarFingerprint.methodOrThrow().apply {
            val backGroundColorIndex = indexOfBackGroundColor(this)
            val viewRegister =
                getInstruction<FiveRegisterInstruction>(backGroundColorIndex).registerC
            val colorRegister =
                getInstruction<FiveRegisterInstruction>(backGroundColorIndex).registerD

            replaceInstruction(
                backGroundColorIndex,
                "invoke-static {v$viewRegister, v$colorRegister}, $EXTENSION_CLASS_DESCRIPTOR->" +
                        "setLithoSnackBarBackgroundColor(Landroid/widget/FrameLayout;I)V"
            )
            setBackground(backGroundColorIndex + 2, viewRegister)

            implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    instruction.opcode == Opcode.CHECK_CAST &&
                            (instruction as? ReferenceInstruction)?.reference?.toString() == "Landroid/widget/FrameLayout;"
                }
                .map { (index, _) -> index }
                .reversed()
                .forEach { index ->
                    val register =
                        getInstruction<OneRegisterInstruction>(index).registerA

                    setBackground(index + 1, register)
                }

            findMethodOrThrow(definingClass).apply {
                val contextIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>()?.type == "Landroid/content/Context;"
                }
                val contextRegister =
                    getInstruction<TwoRegisterInstruction>(contextIndex).registerA

                addInstructions(
                    contextIndex, """
                        invoke-static {v$contextRegister}, $EXTENSION_CLASS_DESCRIPTOR->invertSnackBarTheme(Landroid/content/Context;)Landroid/content/Context;
                        move-result-object v$contextRegister
                        """
                )

                val viewIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>()?.type == "Landroid/widget/FrameLayout;"
                }
                val viewRegister =
                    getInstruction<TwoRegisterInstruction>(viewIndex).registerA

                addInstructions(
                    viewIndex,
                    "invoke-static {v$viewRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideLithoSnackBar(Landroid/widget/FrameLayout;)V"
                )
            }
        }

        addDrawableColorHook("$EXTENSION_CLASS_DESCRIPTOR->getLithoColor(I)I", true)
        addSpanFilter(FILTER_CLASS_DESCRIPTOR)
    }
}

@Suppress("unused")
val snackBarComponentsPatch = resourcePatch(
    SNACK_BAR_COMPONENTS.title,
    SNACK_BAR_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        snackBarComponentsBytecodePatch,
    )

    val ytBackgroundColorDark = "@color/yt_black3"
    val ytBackgroundColorLight = "@color/yt_white3"

    val availableDarkTheme = mapOf(
        "YouTube Dark" to ytBackgroundColorDark,
        "Amoled Black" to "@android:color/black",
        "Catppuccin (Mocha)" to "#FF181825",
        "Dark Pink" to "#FF290025",
        "Dark Blue" to "#FF001029",
        "Dark Green" to "#FF002905",
        "Dark Yellow" to "#FF282900",
        "Dark Orange" to "#FF291800",
        "Dark Red" to "#FF290000",
    )

    val availableLightTheme = mapOf(
        "YouTube Light" to ytBackgroundColorLight,
        "White" to "@android:color/white",
        "Catppuccin (Latte)" to "#FFE6E9EF",
        "Light Pink" to "#FFFCCFF3",
        "Light Blue" to "#FFD1E0FF",
        "Light Green" to "#FFCCFFCC",
        "Light Yellow" to "#FFFDFFCC",
        "Light Orange" to "#FFFFE6CC",
        "Light Red" to "#FFFFD6D6",
    )

    val cornerRadiusOption = stringOption(
        key = "cornerRadius",
        default = "8.0dip",
        title = "Corner radius",
        description = "Specify a corner radius for the snack bar.",
        required = true,
    )

    val applyCornerRadiusToPlaylistBottomBarOption by booleanOption(
        key = "applyCornerRadiusToPlaylistBottomBar",
        default = false,
        title = "Apply corner radius to playlist bottom bar",
        description = "Whether to apply the same corner radius to the bottom bar of the playlist as the snack bar.",
        required = true
    )

    val darkThemeBackgroundColor = stringOption(
        key = "darkThemeBackgroundColor",
        default = ytBackgroundColorDark,
        values = availableDarkTheme,
        title = "Dark theme background color",
        description = "Specify a background color for the snack bar. You can specify hex color (#AARRGGBB) or color resource reference.",
        required = true,
    )

    val lightThemeBackgroundColor = stringOption(
        key = "lightThemeBackgroundColor",
        default = ytBackgroundColorLight,
        values = availableLightTheme,
        title = "Light theme background color",
        description = "Specify a background color for the snack bar. You can specify hex color (#AARRGGBB) or color resource reference.",
        required = true,
    )

    val strokeColorOption = stringOption(
        key = "strokeColor",
        default = "",
        values = mapOf(
            "None" to "",
            "Blue" to "?attr/ytThemedBlue",
            "Chip" to "?attr/ytChipBackground"
        ),
        title = "Stroke color",
        description = "Specify a stroke color for the snack bar. You can specify hex color.",
        required = true,
    )

    execute {

        // Check patch options first.
        val cornerRadius = cornerRadiusOption
            .valueOrThrow()
        val applyCornerRadiusToPlaylistBottomBar =
            applyCornerRadiusToPlaylistBottomBarOption == true
        val darkThemeColor = darkThemeBackgroundColor
            .valueOrThrow()
        val lightThemeColor = lightThemeBackgroundColor
            .valueOrThrow()
        val strokeColor = strokeColorOption
            .valueOrThrow()

        val snackBarColorAttr = "snackBarColor"
        val snackBarColorAttrReference = "?attr/$snackBarColorAttr"
        val snackBarColorDark = "revanced_snack_bar_color_dark"
        val snackBarColorDarkReference = "@color/$snackBarColorDark"
        val snackBarColorLight = "revanced_snack_bar_color_light"
        val snackBarColorLightReference = "@color/$snackBarColorLight"

        document("res/values/colors.xml").use { document ->
            mapOf(
                snackBarColorDark to darkThemeColor,
                snackBarColorLight to lightThemeColor,
            ).forEach { (k, v) ->
                val colorElement = document.createElement("color")

                colorElement.setAttribute("name", k)
                colorElement.textContent = v

                document.getElementsByTagName("resources").item(0)
                    .appendChild(colorElement)
            }
        }

        document("res/values/attrs.xml").use { document ->
            (document.getElementsByTagName("resources").item(0) as Element).appendChild(
                document.createElement("attr").apply {
                    setAttribute("format", "reference|color")
                    setAttribute("name", snackBarColorAttr)
                }
            )
        }

        document("res/values/styles.xml").use { document ->
            mapOf(
                "Base.Theme.YouTube.Dark" to snackBarColorLightReference,
                "Base.Theme.YouTube.Light" to snackBarColorDarkReference,
            ).forEach { (styleName, colorName) ->
                val snackBarColorNode = document.createElement("item")
                snackBarColorNode.setAttribute("name", snackBarColorAttr)
                snackBarColorNode.appendChild(document.createTextNode(colorName))

                document.childNodes.findElementByAttributeValueOrThrow(
                    "name",
                    styleName,
                ).appendChild(snackBarColorNode)
            }
        }

        document("res/drawable/snackbar_rounded_corners_background.xml").use { document ->
            document.getNode("corners").apply {
                arrayOf(
                    "android:bottomLeftRadius",
                    "android:bottomRightRadius",
                    "android:topLeftRadius",
                    "android:topRightRadius",
                ).forEach {
                    attributes.getNamedItem(it).nodeValue = cornerRadius
                }
            }
            document.getNode("solid").apply {
                attributes.getNamedItem("android:color").nodeValue = snackBarColorAttrReference
            }
            if (!strokeColor.isEmpty()) {
                (document.getElementsByTagName("shape").item(0) as Element).appendChild(
                    document.createElement("stroke").apply {
                        setAttribute("android:width", "1.0dip")
                        setAttribute("android:color", strokeColor)
                    }
                )
            }
        }

        document("res/values/dimens.xml").use { document ->
            val resourcesNode = document.documentElement
            val childNodes = resourcesNode.childNodes

            for (i in 0 until childNodes.length) {
                val node = childNodes.item(i) as? Element ?: continue
                val dimenName = node.getAttribute("name")

                if (dimenName.equals("snackbar_corner_radius")) {
                    node.textContent = cornerRadius
                    break
                }
            }
        }

        if (applyCornerRadiusToPlaylistBottomBar) {
            document("res/drawable/playlist_entry_point_corner_drawable.xml").use { document ->
                document.getNode("corners").apply {
                    attributes.getNamedItem("android:radius").nodeValue = cornerRadius
                }
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: SNACK_BAR_COMPONENTS"
            ),
            SNACK_BAR_COMPONENTS
        )

        // endregion

    }
}
