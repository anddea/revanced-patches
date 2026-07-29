/*
 * Copyright (C) 2025-2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 * - kitadai31 (https://github.com/kitadai31)
 * - MondayNitro (https://github.com/MondayNitro)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.patches.youtube.general.snackbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.colorOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.drawable.addDrawableColorHook
import app.morphe.patches.shared.drawable.drawableColorHookPatch
import app.morphe.patches.shared.spans.addSpanFilter
import app.morphe.patches.shared.spans.inclusiveSpanPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
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
                    invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->setLithoSnackBarView(Landroid/view/View;)V
                    """, ExternalLabel("show", getInstruction(0))
            )
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
            addInstruction(
                0,
                "invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->enterLithoSnackBarCreation()V"
            )

            implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    instruction.opcode == Opcode.RETURN_OBJECT || instruction.opcode == Opcode.RETURN_VOID
                }
                .map { (index, _) -> index }
                .reversed()
                .forEach { index ->
                    addInstruction(
                        index,
                        "invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->exitLithoSnackBarCreation()V"
                    )
                }

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
                    viewIndex, """
                        invoke-static {v$viewRegister}, $EXTENSION_CLASS_DESCRIPTOR->setLithoSnackBarView(Landroid/view/View;)V
                        invoke-static {v$viewRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideLithoSnackBar(Landroid/widget/FrameLayout;)V
                        """
                )
            }
        }

        listOf(
            LegacySnackBarConstructorFingerprint,
            YouTubeSnackBarConstructorFingerprint
        ).forEach { fingerprint ->
            fingerprint.method.addInstructions(
                1, """
                    invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->invertSnackBarTheme(Landroid/content/Context;)Landroid/content/Context;
                    move-result-object p1
                    """
            )
        }

        addDrawableColorHook("$EXTENSION_CLASS_DESCRIPTOR->getLithoColor(Landroid/graphics/drawable/Drawable;I)I", true, true)
        addSpanFilter(FILTER_CLASS_DESCRIPTOR)
    }
}

@Suppress("unused")
val snackBarComponentsPatch = resourcePatch(
    SNACK_BAR_COMPONENTS.title,
    SNACK_BAR_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        settingsPatch,
        snackBarComponentsBytecodePatch,
    )

    val ytBackgroundColorDark = "@color/yt_black3"
    val ytBackgroundColorLight = "@color/yt_white3"

    val availableDarkTheme = mapOf(
        "YouTube Dark" to ytBackgroundColorDark,
        "Amoled Black" to "@android:color/black",
        "Material You (Neutral)" to "@android:color/system_neutral1_900",
        "Material You - Primary" to "@android:color/system_accent1_800",
        "Material You - Secondary" to "@android:color/system_accent2_800",
        "Material You - Tertiary" to "@android:color/system_accent3_800",
        "Modern YouTube" to "#FF0F0F0F",
        "Classic (Old YouTube)" to "#FF212121",
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
        "Material You (Neutral)" to "@android:color/system_neutral1_100",
        "Material You - Primary" to "@android:color/system_accent1_200",
        "Material You - Secondary" to "@android:color/system_accent2_200",
        "Material You - Tertiary" to "@android:color/system_accent3_200",
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

    val darkThemeBackgroundColor = colorOption(
        key = "darkThemeBackgroundColor",
        default = ytBackgroundColorDark,
        values = availableDarkTheme,
        title = "Dark theme background color",
        description = "Specify a background color for the snack bar. You can specify hex color (#AARRGGBB) or color resource reference.",
        required = true,
    )

    val lightThemeBackgroundColor = colorOption(
        key = "lightThemeBackgroundColor",
        default = ytBackgroundColorLight,
        values = availableLightTheme,
        title = "Light theme background color",
        description = "Specify a background color for the snack bar. You can specify hex color (#AARRGGBB) or color resource reference.",
        required = true,
    )

    val strokeColorOption = colorOption(
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
