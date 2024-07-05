package app.revanced.patches.youtube.general.miniplayer

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerDimensionsCalculatorParentFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerModernAddViewListenerFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerModernCloseButtonFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerModernConstructorFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerModernConstructorFingerprint.isMultiConstructorMethod
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerModernDragAndDropFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerModernEnabledFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerModernExpandButtonFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerModernExpandCloseDrawablesFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerModernForwardButtonFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerModernOverlayViewFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerModernRewindButtonFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerModernViewParentFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerOverrideFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerOverrideNoContextFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.MiniplayerResponseModelSizeCheckFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.YouTubePlayerOverlaysLayoutFingerprint
import app.revanced.patches.youtube.general.miniplayer.fingerprints.YouTubePlayerOverlaysLayoutFingerprint.YOUTUBE_PLAYER_OVERLAYS_LAYOUT_CLASS_NAME
import app.revanced.patches.youtube.utils.compatibility.Constants
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_PATH
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ModernMiniPlayerClose
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ModernMiniPlayerExpand
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ModernMiniPlayerForwardButton
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ModernMiniPlayerRewindButton
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ScrimOverlay
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.YtOutlinePictureInPictureWhite
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.YtOutlineXWhite
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.findOpcodeIndicesReversed
import app.revanced.util.fingerprint.LiteralValueFingerprint
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfWideLiteralInstructionOrThrow
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
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

// YT uses "Miniplayer" without a space between 'mini' and 'player: https://support.google.com/youtube/answer/9162927.
@Suppress("unused", "SpellCheckingInspection")
object MiniplayerPatch : BaseBytecodePatch(
    name = "Miniplayer",
    description = "Adds options to change the in app minimized player, " +
            "and if patching target 19.16+ adds options to use modern miniplayers.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = Constants.COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        MiniplayerDimensionsCalculatorParentFingerprint,
        MiniplayerResponseModelSizeCheckFingerprint,
        MiniplayerOverrideFingerprint,
        MiniplayerModernConstructorFingerprint,
        MiniplayerModernDragAndDropFingerprint,
        MiniplayerModernEnabledFingerprint,
        MiniplayerModernViewParentFingerprint,
        YouTubePlayerOverlaysLayoutFingerprint,
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR = "$GENERAL_PATH/MiniplayerPatch;"

    override fun execute(context: BytecodeContext) {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: GENERAL"
        )

        // Modern mini player is only present and functional in 19.15+.
        // Resource is not present in older versions. Using it to determine, if patching an old version.
        val isPatchingOldVersion = !SettingsPatch.upward1912

        // From 19.12 to 19.16 using mixed up drawables for tablet modern.
        val shouldFixMixedUpDrawables = YtOutlineXWhite > 0 && YtOutlinePictureInPictureWhite > 0

        // region Enable tablet miniplayer.

        MiniplayerOverrideNoContextFingerprint.resolve(
            context,
            MiniplayerDimensionsCalculatorParentFingerprint.resultOrThrow().classDef
        )
        MiniplayerOverrideNoContextFingerprint.resultOrThrow().mutableMethod.apply {
            findReturnIndicesReversed().forEach { index ->
                insertLegacyTabletMiniplayerOverride(
                    index
                )
            }
        }

        // endregion

        // region Legacy tablet Miniplayer hooks.

        MiniplayerOverrideFingerprint.resultOrThrow().let {
            val appNameStringIndex = it.scanResult.stringsScanResult!!.matches.first().index + 2

            it.mutableMethod.apply {
                val walkerMethod = getWalkerMethod(context, appNameStringIndex)

                walkerMethod.apply {
                    findReturnIndicesReversed().forEach { index ->
                        insertLegacyTabletMiniplayerOverride(
                            index
                        )
                    }
                }
            }
        }

        MiniplayerResponseModelSizeCheckFingerprint.resultOrThrow().let {
            it.mutableMethod.insertLegacyTabletMiniplayerOverride(it.scanResult.patternScanResult!!.endIndex)
        }

        if (isPatchingOldVersion) {
            settingArray += "SETTINGS: MINIPLAYER_TYPE_LEGACY"
            SettingsPatch.addPreference(settingArray)

            SettingsPatch.updatePatchStatus(this)

            // Return here, as patch below is only intended for new versions of the app.
            return
        }

        // endregion


        // region Enable modern miniplayer.

        MiniplayerModernConstructorFingerprint.resultOrThrow().mutableClass.methods.forEach {
            it.apply {
                if (MethodUtil.isConstructor(it)) {
                    val iPutIndex = indexOfFirstInstructionOrThrow {
                        this.opcode == Opcode.IPUT && this.getReference<FieldReference>()?.type == "I"
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

        if (SettingsPatch.upward1925) {
            MiniplayerModernEnabledFingerprint.literalInstructionBooleanHook(
                45622882,
                "$INTEGRATIONS_CLASS_DESCRIPTOR->getModernMiniplayerOverride(Z)Z"
            )
        }

        // endregion

        // region Enable double tap action.

        if (SettingsPatch.upward1925) {
            MiniplayerModernConstructorFingerprint.literalInstructionBooleanHook(
                45628823,
                "$INTEGRATIONS_CLASS_DESCRIPTOR->enableMiniplayerDoubleTapAction()Z"
            )
            MiniplayerModernConstructorFingerprint.literalInstructionBooleanHook(
                45630429,
                "$INTEGRATIONS_CLASS_DESCRIPTOR->getModernMiniplayerOverride(Z)Z"
            )
            settingArray += "SETTINGS: MINIPLAYER_DOUBLE_TAP_ACTION"
        }

        // endregion

        val miniplayerModernViewParentClassDef =
            MiniplayerModernViewParentFingerprint.resultOrThrow().classDef

        // region Fix 19.16 using mixed up drawables for tablet modern.
        // YT fixed this mistake in 19.17.
        // Fix this, by swapping the drawable resource values with each other.
        if (shouldFixMixedUpDrawables) {
            MiniplayerModernExpandCloseDrawablesFingerprint.apply {
                resolve(
                    context,
                    miniplayerModernViewParentClassDef
                )
            }.resultOrThrow().mutableMethod.apply {
                listOf(
                    YtOutlinePictureInPictureWhite to YtOutlineXWhite,
                    YtOutlineXWhite to YtOutlinePictureInPictureWhite,
                ).forEach { (originalResource, replacementResource) ->
                    val imageResourceIndex = indexOfWideLiteralInstructionOrThrow(originalResource)
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
                MiniplayerModernExpandButtonFingerprint,
                ModernMiniPlayerExpand,
                "hideMiniplayerExpandClose"
            ),
            Triple(
                MiniplayerModernCloseButtonFingerprint,
                ModernMiniPlayerClose,
                "hideMiniplayerExpandClose"
            ),
            Triple(
                MiniplayerModernRewindButtonFingerprint,
                ModernMiniPlayerRewindButton,
                "hideMiniplayerRewindForward"
            ),
            Triple(
                MiniplayerModernForwardButtonFingerprint,
                ModernMiniPlayerForwardButton,
                "hideMiniplayerRewindForward"
            ),
            Triple(MiniplayerModernOverlayViewFingerprint, ScrimOverlay, "adjustMiniplayerOpacity")
        ).forEach { (fingerprint, literalValue, methodName) ->
            fingerprint.resolve(
                context,
                miniplayerModernViewParentClassDef
            )

            fingerprint.hookInflatedView(
                literalValue,
                "Landroid/widget/ImageView;",
                "$INTEGRATIONS_CLASS_DESCRIPTOR->$methodName(Landroid/widget/ImageView;)V"
            )
        }

        MiniplayerModernAddViewListenerFingerprint.apply {
            resolve(
                context,
                miniplayerModernViewParentClassDef
            )
        }.resultOrThrow().mutableMethod.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static { p1 }, $INTEGRATIONS_CLASS_DESCRIPTOR->hideMiniplayerSubTexts(Landroid/view/View;)Z
                    move-result v0
                    if-nez v0, :hidden
                    """,
                ExternalLabel("hidden", getInstruction(implementation!!.instructions.lastIndex))
            )
        }


        // Modern 2 has a broken overlay subtitle view that is always present.
        // Modern 2 uses the same overlay controls as the regular video player,
        // and the overlay views are added at runtime.
        // Add a hook to the overlay class, and pass the added views to integrations.
        YouTubePlayerOverlaysLayoutFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                it.mutableClass.methods.add(
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
                                invoke-static { p1 }, $INTEGRATIONS_CLASS_DESCRIPTOR->playerOverlayGroupCreated(Landroid/view/View;)V
                                return-void
                                """,
                        )
                    }
                )
            }
        }

        // endregion


        // region Enable drag and drop.

        if (SettingsPatch.upward1923) {
            MiniplayerModernDragAndDropFingerprint.literalInstructionBooleanHook(
                45628752,
                "$INTEGRATIONS_CLASS_DESCRIPTOR->enableMiniplayerDragAndDrop()Z"
            )
            settingArray += "SETTINGS: MINIPLAYER_DRAG_AND_DROP"
        }

        // endregion

        settingArray += "SETTINGS: MINIPLAYER_TYPE_MODERN"
        SettingsPatch.addPreference(settingArray)

        SettingsPatch.updatePatchStatus(this)
    }

    private fun Method.findReturnIndicesReversed() = findOpcodeIndicesReversed(Opcode.RETURN)

    /**
     * Adds an override to force legacy tablet miniplayer to be used or not used.
     */
    private fun MutableMethod.insertLegacyTabletMiniplayerOverride(index: Int) {
        insertBooleanOverride(index, "getLegacyTabletMiniplayerOverride")
    }

    /**
     * Adds an override to force modern miniplayer to be used or not used.
     */
    private fun MutableMethod.insertModernMiniplayerOverride(index: Int) {
        insertBooleanOverride(index, "getModernMiniplayerOverride")
    }

    private fun MutableMethod.insertBooleanOverride(index: Int, methodName: String) {
        val register = getInstruction<OneRegisterInstruction>(index).registerA
        addInstructions(
            index,
            """
                invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR->$methodName(Z)Z
                move-result v$register
            """
        )
    }

    /**
     * Adds an override to specify which modern miniplayer is used.
     */
    private fun MutableMethod.insertModernMiniplayerTypeOverride(iPutIndex: Int) {
        val targetInstruction = getInstruction<TwoRegisterInstruction>(iPutIndex)
        val targetReference = (targetInstruction as ReferenceInstruction).reference

        addInstructions(
            iPutIndex + 1, """
                invoke-static { v${targetInstruction.registerA} }, $INTEGRATIONS_CLASS_DESCRIPTOR->getModernMiniplayerOverrideType(I)I
                move-result v${targetInstruction.registerA}
                # Original instruction
                iput v${targetInstruction.registerA}, v${targetInstruction.registerB}, $targetReference 
            """
        )
        removeInstruction(iPutIndex)
    }

    private fun LiteralValueFingerprint.hookInflatedView(
        literalValue: Long,
        hookedClassType: String,
        integrationsMethodName: String,
    ) {
        resultOrThrow().mutableMethod.apply {
            val imageViewIndex = indexOfFirstInstructionOrThrow(
                indexOfWideLiteralInstructionOrThrow(literalValue)
            ) {
                opcode == Opcode.CHECK_CAST && getReference<TypeReference>()?.type == hookedClassType
            }

            val register = getInstruction<OneRegisterInstruction>(imageViewIndex).registerA
            addInstruction(
                imageViewIndex + 1,
                "invoke-static { v$register }, $integrationsMethodName"
            )
        }
    }
}
