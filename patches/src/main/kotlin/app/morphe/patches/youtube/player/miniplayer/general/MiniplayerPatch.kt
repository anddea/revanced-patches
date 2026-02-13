package app.morphe.patches.youtube.player.miniplayer.general

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.MINIPLAYER
import app.morphe.patches.youtube.utils.playservice.is_19_15_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_17_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_23_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_25_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_26_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_29_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_34_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_36_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_43_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_03_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.resourceid.modernMiniPlayerClose
import app.morphe.patches.youtube.utils.resourceid.modernMiniPlayerExpand
import app.morphe.patches.youtube.utils.resourceid.modernMiniPlayerForwardButton
import app.morphe.patches.youtube.utils.resourceid.modernMiniPlayerOverlayActionButton
import app.morphe.patches.youtube.utils.resourceid.modernMiniPlayerRewindButton
import app.morphe.patches.youtube.utils.resourceid.scrimOverlay
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.resourceid.ytOutlinePictureInPictureWhite
import app.morphe.patches.youtube.utils.resourceid.ytOutlineXWhite
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.fingerprint.mutableClassOrThrow
import app.morphe.util.getReference
import app.morphe.util.getWalkerMethod
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/MiniplayerPatch;"

// YT uses "Miniplayer" without a space between 'mini' and 'player: https://support.google.com/youtube/answer/9162927.
@Suppress("unused", "SpellCheckingInspection")
val miniplayerPatch = bytecodePatch(
    MINIPLAYER.title,
    MINIPLAYER.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        sharedResourceIdPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: PLAYER",
            "SETTINGS: MINIPLAYER_COMPONENTS"
        )

        fun Method.findReturnIndicesReversed() =
            findInstructionIndicesReversedOrThrow(Opcode.RETURN)

        fun MutableMethod.insertBooleanOverride(index: Int, methodName: String) {
            val register = getInstruction<OneRegisterInstruction>(index).registerA
            addInstructions(
                index,
                """
                invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->$methodName(Z)Z
                move-result v$register
                """
            )
        }

        /**
         * Adds an override to force legacy tablet miniplayer to be used or not used.
         */
        fun MutableMethod.insertLegacyTabletMiniplayerOverride(index: Int) {
            insertBooleanOverride(index, "getLegacyTabletMiniplayerOverride")
        }

        /**
         * Adds an override to force modern miniplayer to be used or not used.
         */
        fun MutableMethod.insertModernMiniplayerOverride(index: Int) {
            insertBooleanOverride(index, "getModernMiniplayerOverride")
        }

        /**
         * Adds an override to specify which modern miniplayer is used.
         */
        fun MutableMethod.insertModernMiniplayerTypeOverride(iPutIndex: Int) {
            val register = getInstruction<TwoRegisterInstruction>(iPutIndex).registerA

            addInstructionsAtControlFlowLabel(
                iPutIndex,
                """
                    invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->getModernMiniplayerOverrideType(I)I
                    move-result v$register
                """,
            )
        }

        // Modern mini player is only present and functional in 19.15+.
        // Resource is not present in older versions. Using it to determine, if patching an old version.
        val isPatchingOldVersion = !is_19_15_or_greater

        // From 19.15 to 19.16 using mixed up drawables for tablet modern.
        val shouldFixMixedUpDrawables = ytOutlineXWhite > 0 && ytOutlinePictureInPictureWhite > 0

        // From 19.15 to 19.34 swipe to dismiss miniplayer is not working.
        val shouldFixSwipeToDismiss = is_19_15_or_greater && !is_19_34_or_greater

        // region Enable tablet miniplayer.

        miniplayerOverrideNoContextFingerprint.methodOrThrow(
            miniplayerDimensionsCalculatorParentFingerprint
        ).apply {
            findReturnIndicesReversed().forEach { index ->
                insertLegacyTabletMiniplayerOverride(
                    index
                )
            }
        }

        // endregion

        // region Legacy tablet Miniplayer hooks.

        miniplayerOverrideFingerprint.matchOrThrow().let {
            it.method.apply {
                val stringIndex = it.stringMatches!!.first().index
                val walkerIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                    val reference = getReference<MethodReference>()
                    reference?.returnType == "Z" &&
                            reference.parameterTypes.size == 1 &&
                            reference.parameterTypes.firstOrNull() == "Landroid/content/Context;"
                }
                val walkerMethod = getWalkerMethod(walkerIndex)

                walkerMethod.apply {
                    findReturnIndicesReversed().forEach { index ->
                        insertLegacyTabletMiniplayerOverride(
                            index
                        )
                    }
                }
            }
        }

        miniplayerResponseModelSizeCheckFingerprint.matchOrThrow().let {
            it.method.insertLegacyTabletMiniplayerOverride(it.instructionMatches.last().index)
        }

        if (isPatchingOldVersion) {
            settingArray += "SETTINGS: MINIPLAYER_TYPE_19_14"
            addPreference(settingArray, MINIPLAYER)

            // Return here, as patch below is only intended for new versions of the app.
            return@execute
        }

        // endregion

        // region Enable modern miniplayer.

        miniplayerModernConstructorFingerprint.mutableClassOrThrow().methods.forEach {
            it.apply {
                if (AccessFlags.CONSTRUCTOR.isSet(accessFlags)) {
                    val iPutIndex = indexOfFirstInstructionOrThrow {
                        this.opcode == Opcode.IPUT &&
                                this.getReference<FieldReference>()?.type == "I"
                    }

                    insertModernMiniplayerTypeOverride(iPutIndex)
                } else {
                    findReturnIndicesReversed().forEach { index ->
                        insertModernMiniplayerOverride(
                            index
                        )
                    }
                }
            }
        }

        if (is_19_23_or_greater) {
            miniplayerModernConstructorFingerprint.injectLiteralInstructionBooleanCall(
                MINIPLAYER_DRAG_DROP_FEATURE_KEY,
                "$EXTENSION_CLASS_DESCRIPTOR->getMiniplayerDragAndDrop(Z)Z"
            )
            settingArray += "SETTINGS: MINIPLAYER_DRAG_AND_DROP"
        }

        if (is_19_25_or_greater) {
            miniplayerModernConstructorFingerprint.injectLiteralInstructionBooleanCall(
                MINIPLAYER_MODERN_FEATURE_LEGACY_KEY,
                "$EXTENSION_CLASS_DESCRIPTOR->getModernMiniplayerOverride(Z)Z"
            )

            miniplayerModernConstructorFingerprint.injectLiteralInstructionBooleanCall(
                MINIPLAYER_MODERN_FEATURE_KEY,
                "$EXTENSION_CLASS_DESCRIPTOR->getModernFeatureFlagsActiveOverride(Z)Z"
            )

            miniplayerModernConstructorFingerprint.injectLiteralInstructionBooleanCall(
                MINIPLAYER_DOUBLE_TAP_FEATURE_KEY,
                "$EXTENSION_CLASS_DESCRIPTOR->getMiniplayerDoubleTapAction(Z)Z"
            )

            if (!is_19_29_or_greater) {
                settingArray += "SETTINGS: MINIPLAYER_DOUBLE_TAP_ACTION"
            }
        }

        if (is_19_26_or_greater) {
            miniplayerModernConstructorFingerprint.methodOrThrow().apply {
                val literalIndex = indexOfFirstLiteralInstructionOrThrow(
                    MINIPLAYER_INITIAL_SIZE_FEATURE_KEY,
                )
                val targetIndex = indexOfFirstInstructionOrThrow(literalIndex, Opcode.LONG_TO_INT)
                val register = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1,
                    """
                        invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->getMiniplayerDefaultSize(I)I
                        move-result v$register
                        """,
                )
            }

            // Override a minimum size constant.
            miniplayerMinimumSizeFingerprint.methodOrThrow().apply {
                val index = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.CONST_16 &&
                            (this as NarrowLiteralInstruction).narrowLiteral == 192
                }
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                // Smaller sizes can be used, but the miniplayer will always start in size 170 if set any smaller.
                // The 170 initial limit probably could be patched to allow even smaller initial sizes,
                // but 170 is already half the horizontal space and smaller does not seem useful.
                replaceInstruction(index, "const/16 v$register, 170")
            }

            settingArray += "SETTINGS: MINIPLAYER_OVERLAY_BUTTONS_19_26"
            settingArray += "SETTINGS: MINIPLAYER_WIDTH_DIP"
        } else {
            settingArray += "SETTINGS: MINIPLAYER_OVERLAY_BUTTONS_19_25"
            settingArray += "SETTINGS: MINIPLAYER_REWIND_FORWARD"
        }

        if (is_19_36_or_greater) {
            miniplayerModernConstructorFingerprint.injectLiteralInstructionBooleanCall(
                MINIPLAYER_ROUNDED_CORNERS_FEATURE_KEY,
                "$EXTENSION_CLASS_DESCRIPTOR->getRoundedCorners(Z)Z"
            )

            settingArray += "SETTINGS: MINIPLAYER_ROUNDED_CORNERS"
        }

        if (is_19_43_or_greater) {
            miniplayerOnCloseHandlerFingerprint.injectLiteralInstructionBooleanCall(
                MINIPLAYER_DISABLED_FEATURE_KEY,
                "$EXTENSION_CLASS_DESCRIPTOR->getMiniplayerOnCloseHandler(Z)Z"
            )

            miniplayerModernConstructorFingerprint.injectLiteralInstructionBooleanCall(
                MINIPLAYER_HORIZONTAL_DRAG_FEATURE_KEY,
                "$EXTENSION_CLASS_DESCRIPTOR->getHorizontalDrag(Z)Z"
            )

            miniplayerModernConstructorFingerprint.injectLiteralInstructionBooleanCall(
                MINIPLAYER_ANIMATED_EXPAND_FEATURE_KEY,
                "$EXTENSION_CLASS_DESCRIPTOR->getMaximizeAnimation(Z)Z"
            )

            settingArray += "SETTINGS: MINIPLAYER_HORIZONTAL_DRAG"
        }

        settingArray += if (is_20_03_or_greater) {
            "SETTINGS: MINIPLAYER_TYPE_20_03"
        } else if (is_19_43_or_greater) {
            "SETTINGS: MINIPLAYER_TYPE_19_43"
        } else {
            "SETTINGS: MINIPLAYER_TYPE_19_16"
        }

        // endregion

        // region Fix 19.16 using mixed up drawables for tablet modern.
        // YT fixed this mistake in 19.17.
        // Fix this, by swapping the drawable resource values with each other.
        if (shouldFixMixedUpDrawables) {
            miniplayerModernExpandCloseDrawablesFingerprint.methodOrThrow(
                miniplayerModernViewParentFingerprint
            ).apply {
                listOf(
                    ytOutlinePictureInPictureWhite to ytOutlineXWhite,
                    ytOutlineXWhite to ytOutlinePictureInPictureWhite,
                ).forEach { (originalResource, replacementResource) ->
                    val imageResourceIndex =
                        indexOfFirstLiteralInstructionOrThrow(originalResource)
                    val register =
                        getInstruction<OneRegisterInstruction>(imageResourceIndex).registerA

                    replaceInstruction(imageResourceIndex, "const v$register, $replacementResource")
                }
            }
        }

        // endregion

        // region Fix 19.16 swiping down miniplayer does not dismiss.

        if (shouldFixSwipeToDismiss) {
            miniplayerModernSwipeToDismissFingerprint.injectLiteralInstructionBooleanCall(
                MINIPLAYER_SWIPE_TO_DISMISS_FEATURE_KEY,
                "0x0"
            )
        }

        // endregion

        // region Add hooks to hide tablet modern miniplayer buttons.

        listOf(
            Triple(
                miniplayerModernExpandButtonFingerprint,
                modernMiniPlayerExpand,
                "hideMiniplayerExpandClose"
            ),
            Triple(
                miniplayerModernCloseButtonFingerprint,
                modernMiniPlayerClose,
                "hideMiniplayerExpandClose"
            ),
            Triple(
                miniplayerModernActionButtonFingerprint,
                modernMiniPlayerOverlayActionButton,
                "hideMiniplayerActionButton"
            ),
            Triple(
                miniplayerModernRewindButtonFingerprint,
                modernMiniPlayerRewindButton,
                "hideMiniplayerRewindForward"
            ),
            Triple(
                miniplayerModernForwardButtonFingerprint,
                modernMiniPlayerForwardButton,
                "hideMiniplayerRewindForward"
            ),
            Triple(
                miniplayerModernOverlayViewFingerprint,
                scrimOverlay,
                "adjustMiniplayerOpacity"
            )
        ).forEach { (fingerprint, literalValue, methodName) ->
            fingerprint.methodOrThrow(miniplayerModernViewParentFingerprint).apply {
                val literalIndex = indexOfFirstLiteralInstructionOrThrow(literalValue)
                val checkCastIndex = indexOfFirstInstruction(literalIndex) {
                    opcode == Opcode.CHECK_CAST &&
                            getReference<TypeReference>()?.type == "Landroid/widget/ImageView;"
                }
                val viewIndex = if (checkCastIndex >= 0) {
                    checkCastIndex
                } else {
                    indexOfFirstInstructionOrThrow(literalIndex, Opcode.MOVE_RESULT_OBJECT)
                }
                val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA

                addInstruction(
                    viewIndex + 1,
                    "invoke-static { v$viewRegister }, $EXTENSION_CLASS_DESCRIPTOR->$methodName(Landroid/view/View;)V"
                )
            }
        }

        miniplayerModernAddViewListenerFingerprint.methodOrThrow(
            miniplayerModernViewParentFingerprint
        ).addInstruction(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->" +
                    "hideMiniplayerSubTexts(Landroid/view/View;)V",
        )

        // Modern 2 has a broken overlay subtitle view that is always present.
        // Modern 2 uses the same overlay controls as the regular video player,
        // and the overlay views are added at runtime.
        // Add a hook to the overlay class, and pass the added views to extension.
        // Problem is fixed in 19.21+
        //
        // NOTE: Modern 2 uses the same video UI as the regular player except resized to smaller.
        // This patch code could be used to hide other player overlays that do not use Litho.
        if (!is_19_17_or_greater) {
            youTubePlayerOverlaysLayoutFingerprint.matchOrThrow().let {
                it.method.apply {
                    it.classDef.methods.add(
                        ImmutableMethod(
                            YOUTUBE_PLAYER_OVERLAYS_LAYOUT_CLASS_NAME,
                            "addView",
                            listOf(
                                ImmutableMethodParameter("Landroid/view/View;", annotations, null),
                                ImmutableMethodParameter("I", annotations, null),
                                ImmutableMethodParameter(
                                    "Landroid/view/ViewGroup\$LayoutParams;",
                                    annotations,
                                    null
                                ),
                            ),
                            "V",
                            AccessFlags.PUBLIC.value,
                            annotations,
                            null,
                            MutableMethodImplementation(4),
                        ).toMutable().apply {
                            addInstructions(
                                """
                                    invoke-super { p0, p1, p2, p3 }, Landroid/view/ViewGroup;->addView(Landroid/view/View;ILandroid/view/ViewGroup${'$'}LayoutParams;)V
                                    invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->playerOverlayGroupCreated(Landroid/view/View;)V
                                    return-void
                                    """,
                            )
                        }
                    )
                }
            }
        }

        // endregion

        settingArray += "SETTINGS: MINIPLAYER_TYPE_MODERN"

        // region add settings

        addPreference(settingArray, MINIPLAYER)

        // endregion

    }
}
