package app.revanced.patches.youtube.general.miniplayer

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.MINIPLAYER
import app.revanced.patches.youtube.utils.playservice.is_19_15_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_23_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_25_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.resourceid.modernMiniPlayerClose
import app.revanced.patches.youtube.utils.resourceid.modernMiniPlayerExpand
import app.revanced.patches.youtube.utils.resourceid.modernMiniPlayerForwardButton
import app.revanced.patches.youtube.utils.resourceid.modernMiniPlayerRewindButton
import app.revanced.patches.youtube.utils.resourceid.scrimOverlay
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.ytOutlinePictureInPictureWhite
import app.revanced.patches.youtube.utils.resourceid.ytOutlineXWhite
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/MiniplayerPatch;"

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
            "PREFERENCE_SCREEN: GENERAL"
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
            val targetInstruction = getInstruction<TwoRegisterInstruction>(iPutIndex)
            val targetReference = (targetInstruction as ReferenceInstruction).reference

            addInstructions(
                iPutIndex + 1, """
                invoke-static { v${targetInstruction.registerA} }, $EXTENSION_CLASS_DESCRIPTOR->getModernMiniplayerOverrideType(I)I
                move-result v${targetInstruction.registerA}
                # Original instruction
                iput v${targetInstruction.registerA}, v${targetInstruction.registerB}, $targetReference 
            """
            )
            removeInstruction(iPutIndex)
        }

        fun Pair<String, Fingerprint>.hookInflatedView(
            literalValue: Long,
            hookedClassType: String,
            extensionMethodName: String,
        ) {
            methodOrThrow(miniplayerModernViewParentFingerprint).apply {
                val imageViewIndex = indexOfFirstInstructionOrThrow(
                    indexOfFirstLiteralInstructionOrThrow(literalValue)
                ) {
                    opcode == Opcode.CHECK_CAST &&
                            getReference<TypeReference>()?.type == hookedClassType
                }

                val register = getInstruction<OneRegisterInstruction>(imageViewIndex).registerA
                addInstruction(
                    imageViewIndex + 1,
                    "invoke-static { v$register }, $extensionMethodName"
                )
            }
        }

        // Modern mini player is only present and functional in 19.15+.
        // Resource is not present in older versions. Using it to determine, if patching an old version.
        val isPatchingOldVersion = !is_19_15_or_greater

        // From 19.15 to 19.16 using mixed up drawables for tablet modern.
        val shouldFixMixedUpDrawables = ytOutlineXWhite > 0 && ytOutlinePictureInPictureWhite > 0

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
            val appNameStringIndex = it.stringMatches!!.first().index + 2

            it.method.apply {
                val walkerMethod = getWalkerMethod(appNameStringIndex)

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
            it.method.insertLegacyTabletMiniplayerOverride(it.patternMatch!!.endIndex)
        }

        if (isPatchingOldVersion) {
            settingArray += "SETTINGS: MINIPLAYER_TYPE_LEGACY"
            addPreference(settingArray, MINIPLAYER)

            // Return here, as patch below is only intended for new versions of the app.
            return@execute
        }

        // endregion

        // region Enable modern miniplayer.

        miniplayerModernConstructorFingerprint.mutableClassOrThrow().methods.forEach {
            it.apply {
                if (MethodUtil.isConstructor(it)) {
                    val iPutIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.IPUT &&
                                getReference<FieldReference>()?.type == "I"
                    }

                    insertModernMiniplayerTypeOverride(iPutIndex)
                } else if (isMultiConstructorMethod()) {
                    findReturnIndicesReversed().forEach { index ->
                        insertModernMiniplayerOverride(
                            index
                        )
                    }
                }
            }
        }

        if (is_19_25_or_greater) {
            miniplayerModernEnabledFingerprint.injectLiteralInstructionBooleanCall(
                45622882L,
                "$EXTENSION_CLASS_DESCRIPTOR->getModernMiniplayerOverride(Z)Z"
            )
        }

        // endregion

        // region Enable double tap action.

        if (is_19_25_or_greater) {
            miniplayerModernConstructorFingerprint.injectLiteralInstructionBooleanCall(
                45628823L,
                "$EXTENSION_CLASS_DESCRIPTOR->enableMiniplayerDoubleTapAction()Z"
            )
            miniplayerModernConstructorFingerprint.injectLiteralInstructionBooleanCall(
                45630429L,
                "$EXTENSION_CLASS_DESCRIPTOR->getModernMiniplayerOverride(Z)Z"
            )
            settingArray += "SETTINGS: MINIPLAYER_DOUBLE_TAP_ACTION"
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
            fingerprint.hookInflatedView(
                literalValue,
                "Landroid/widget/ImageView;",
                "$EXTENSION_CLASS_DESCRIPTOR->$methodName(Landroid/widget/ImageView;)V"
            )
        }

        miniplayerModernAddViewListenerFingerprint.methodOrThrow(
            miniplayerModernViewParentFingerprint
        ).apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->hideMiniplayerSubTexts(Landroid/view/View;)Z
                    move-result v0
                    if-nez v0, :hidden
                    """,
                ExternalLabel("hidden", getInstruction(implementation!!.instructions.lastIndex))
            )
        }


        // Modern 2 has a broken overlay subtitle view that is always present.
        // Modern 2 uses the same overlay controls as the regular video player,
        // and the overlay views are added at runtime.
        // Add a hook to the overlay class, and pass the added views to extension.
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

        // endregion

        // region Enable drag and drop.

        if (is_19_23_or_greater) {
            miniplayerModernDragAndDropFingerprint.injectLiteralInstructionBooleanCall(
                45628752L,
                "$EXTENSION_CLASS_DESCRIPTOR->enableMiniplayerDragAndDrop()Z"
            )
            settingArray += "SETTINGS: MINIPLAYER_DRAG_AND_DROP"
        }

        // endregion

        settingArray += "SETTINGS: MINIPLAYER_TYPE_MODERN"

        // region add settings

        addPreference(settingArray, MINIPLAYER)

        // endregion

    }
}
