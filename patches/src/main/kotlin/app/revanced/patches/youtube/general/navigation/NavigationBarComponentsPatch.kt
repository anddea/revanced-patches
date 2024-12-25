package app.revanced.patches.youtube.general.navigation

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.navigation.addBottomBarContainerHook
import app.revanced.patches.youtube.utils.navigation.hookNavigationButtonCreated
import app.revanced.patches.youtube.utils.navigation.navigationBarHookPatch
import app.revanced.patches.youtube.utils.patch.PatchList.NAVIGATION_BAR_COMPONENTS
import app.revanced.patches.youtube.utils.playservice.is_19_23_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_25_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_28_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val navigationBarComponentsResourcePatch = resourcePatch(
    description = "navigationBarComponentsResourcePatch"
) {
    dependsOn(versionCheckPatch)

    execute {
        if (is_19_28_or_greater) {
            // Since I couldn't get the Cairo notification filled icon anywhere,
            // I just made it as close as possible.
            arrayOf(
                "xxxhdpi",
                "xxhdpi",
                "xhdpi",
                "hdpi",
                "mdpi"
            ).forEach { dpi ->
                copyResources(
                    "youtube/navigationbuttons",
                    ResourceGroup(
                        "drawable-$dpi",
                        "yt_fill_bell_cairo_black_24.png"
                    )
                )
            }
        }
    }
}

@Suppress("unused")
val navigationBarComponentsPatch = bytecodePatch(
    NAVIGATION_BAR_COMPONENTS.title,
    NAVIGATION_BAR_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        navigationBarComponentsResourcePatch,
        settingsPatch,
        sharedResourceIdPatch,
        navigationBarHookPatch,
        versionCheckPatch,
    )

    execute {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: GENERAL",
            "SETTINGS: HIDE_NAVIGATION_COMPONENTS"
        )

        // region patch for enable translucent navigation bar

        if (is_19_23_or_greater) {
            translucentNavigationBarFingerprint.injectLiteralInstructionBooleanCall(
                TRANSLUCENT_NAVIGATION_BUTTONS_FEATURE_FLAG,
                "$GENERAL_CLASS_DESCRIPTOR->enableTranslucentNavigationBar()Z"
            )

            settingArray += "SETTINGS: TRANSLUCENT_NAVIGATION_BAR"
        }

        if (is_19_25_or_greater) {
            arrayOf(
                Triple(
                    translucentNavigationStatusBarFeatureFlagFingerprint,
                    TRANSLUCENT_NAVIGATION_STATUS_BAR_FEATURE_FLAG,
                    "useTranslucentNavigationStatusBar"
                ),
                Triple(
                    translucentNavigationButtonsFeatureFlagFingerprint,
                    TRANSLUCENT_NAVIGATION_BUTTONS_FEATURE_FLAG,
                    "useTranslucentNavigationButtons"
                ),
                Triple(
                    translucentNavigationButtonsSystemFeatureFlagFingerprint,
                    TRANSLUCENT_NAVIGATION_BUTTONS_SYSTEM_FEATURE_FLAG,
                    "useTranslucentNavigationButtons"
                )
            ).forEach {
                it.first.injectLiteralInstructionBooleanCall(
                    it.second,
                    "$GENERAL_CLASS_DESCRIPTOR->${it.third}(Z)Z"
                )
            }

            translucentStatusBarFingerprint.injectLiteralInstructionBooleanCall(
                TRANSLUCENT_NAVIGATION_STATUS_BAR_FEATURE_FLAG,
                "$GENERAL_CLASS_DESCRIPTOR->enableTranslucentStatusBar()Z"
            )

            translucentNavigationBarSystemFingerprint.injectLiteralInstructionBooleanCall(
                TRANSLUCENT_NAVIGATION_BUTTONS_SYSTEM_FEATURE_FLAG,
                "$GENERAL_CLASS_DESCRIPTOR->enableTranslucentNavigationBar()Z"
            )

            settingArray += "SETTINGS: DISABLE_TRANSLUCENT_STATUS_BAR"
            settingArray += "SETTINGS: TRANSLUCENT_NAVIGATION_BAR"
        }

        // endregion

        // region patch for enable narrow navigation buttons

        arrayOf(
            pivotBarChangedFingerprint,
            pivotBarStyleFingerprint
        ).forEach { fingerprint ->
            fingerprint.matchOrThrow().let {
                it.method.apply {
                    val targetIndex = it.patternMatch!!.startIndex + 1
                    val register = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->enableNarrowNavigationButton(Z)Z
                            move-result v$register
                            """
                    )
                }
            }
        }

        // endregion

        // region patch for hide navigation bar

        addBottomBarContainerHook("$GENERAL_CLASS_DESCRIPTOR->hideNavigationBar(Landroid/view/View;)V")

        // endregion

        // region patch for hide navigation buttons

        autoMotiveFingerprint.methodOrThrow().apply {
            val insertIndex = indexOfFirstStringInstructionOrThrow(ANDROID_AUTOMOTIVE_STRING) - 1
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex, """
                    invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->switchCreateWithNotificationButton(Z)Z
                    move-result v$insertRegister
                    """
            )
        }

        // endregion

        // region patch for hide navigation label

        pivotBarSetTextFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "setText"
                }
                val targetRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerC

                addInstruction(
                    targetIndex,
                    "invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->hideNavigationLabel(Landroid/widget/TextView;)V"
                )
            }
        }

        // endregion

        // region fix for cairo notification icon

        /**
         * The Cairo navigation bar was widely rolled out in YouTube 19.28.42.
         *
         * Unlike Home, Shorts, and Subscriptions, which have Cairo icons,
         * Notifications does not have a Cairo icon.
         *
         * This led to an issue <a href="https://github.com/ReVanced/revanced-patches/issues/4046">revanced-patches#4046</a>,
         * Which was closed as not planned because it was a YouTube issue and not a ReVanced issue.
         *
         * It was not too hard to fix, so it was implemented as a patch.
         */
        if (is_19_28_or_greater) {
            val cairoNotificationEnumReference =
                with(imageEnumConstructorFingerprint.methodOrThrow()) {
                    val stringIndex =
                        indexOfFirstStringInstructionOrThrow(TAB_ACTIVITY_CAIRO_STRING)
                    val cairoNotificationEnumIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                        opcode == Opcode.SPUT_OBJECT
                    }
                    getInstruction<ReferenceInstruction>(cairoNotificationEnumIndex).reference
                }

            setEnumMapFingerprint.methodOrThrow().apply {
                val enumMapIndex = indexOfFirstInstructionReversedOrThrow {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.definingClass == "Ljava/util/EnumMap;" &&
                            reference.name == "put" &&
                            reference.parameterTypes.firstOrNull() == "Ljava/lang/Enum;"
                }
                val (enumMapRegister, enumRegister) = getInstruction<FiveRegisterInstruction>(
                    enumMapIndex
                ).let {
                    Pair(it.registerC, it.registerD)
                }

                addInstructions(
                    enumMapIndex + 1, """
                        sget-object v$enumRegister, $cairoNotificationEnumReference
                        invoke-static {v$enumMapRegister, v$enumRegister}, $GENERAL_CLASS_DESCRIPTOR->setCairoNotificationFilledIcon(Ljava/util/EnumMap;Ljava/lang/Enum;)V
                        """
                )
            }
        }

        // endregion

        // Hook navigation button created, in order to hide them.
        hookNavigationButtonCreated(GENERAL_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(settingArray, NAVIGATION_BAR_COMPONENTS)

        // endregion
    }
}

