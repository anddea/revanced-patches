/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.player.action

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.CLIENT_INFO_CLASS_DESCRIPTOR
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.misc.fix.proto.fixProtoLibraryPatch
import app.morphe.patches.shared.misc.request.buildRequestPatch
import app.morphe.patches.shared.misc.request.hookBuildRequest
import app.morphe.patches.shared.misc.spoof.BuildInnerTubeProtoRequestUriFingerprint
import app.morphe.patches.youtube.shared.WatchNextResponseParserFingerprint
import app.morphe.patches.youtube.utils.auth.authHookPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.componentlist.hookElementList
import app.morphe.patches.youtube.utils.componentlist.lazilyConvertedElementHookPatch
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.morphe.patches.youtube.utils.fix.hype.hypeButtonIconPatch
import app.morphe.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.morphe.patches.youtube.utils.patch.PatchList.HIDE_ACTION_BUTTONS
import app.morphe.patches.youtube.utils.playservice.is_20_30_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.videoid.hookPlayerResponseVideoId
import app.morphe.patches.youtube.video.videoid.videoIdPatch
import app.morphe.util.getReference
import app.morphe.util.insertLiteralOverride
import app.morphe.util.registersUsed
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ActionButtonsFilter;"
private const val ACTION_BUTTONS_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/ActionButtonsPatch;"
private const val EXTENSION_CONFIG_INFO_INTERFACE =
    $$"$$GENERAL_PATH/GeneralPatch$ConfigInfoInterface;"

private object ModernRelateVideoOverlayFingerprint : Fingerprint(
    filters = listOf(
        literal(45614162L)
    )
)

private object RelateVideoOverlayLayoutParamFingerprint : Fingerprint(
    filters = listOf(
        literal(45661108L)
    )
)

private object BuildInnerTubeProtoRequestBodyFingerprint : Fingerprint(
    classFingerprint = BuildInnerTubeProtoRequestUriFingerprint,
    parameters = listOf("L"),
    returnType = "Lcom/google/protobuf/MessageLite;",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = CLIENT_INFO_CLASS_DESCRIPTOR
        )
    )
)

private fun getConfigInfoFingerprint(configInfoClass: String) = object : Fingerprint(
    definingClass = configInfoClass,
    name = "<init>",
    parameters = listOf(),
    returnType = "V",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/String;"
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/String;"
        )
    )
) {}

internal val restoreOldVideoActionBarPatch = bytecodePatch(
    description = "restoreOldVideoActionBarPatch"
) {
    dependsOn(
        settingsPatch,
        buildRequestPatch,
        fixProtoLibraryPatch,
        versionCheckPatch,
    )

    execute {
        if (is_20_30_or_greater) {
            addPreference(
                arrayOf(
                    "PREFERENCE_SCREEN: PLAYER",
                    "PREFERENCE_SCREENS: PLAYER_BUTTONS",
                    "SETTINGS: RESTORE_OLD_VIDEO_ACTION_BAR"
                )
            )

            hookBuildRequest(
                descriptor = "$GENERAL_CLASS_DESCRIPTOR->fixVideoActionBar(Ljava/lang/String;Ljava/util/Map;)Ljava/util/Map;",
                hookHeader = true,
            )

            val configInfoClass = with(BuildInnerTubeProtoRequestBodyFingerprint) {
                val match = instructionMatches.first()
                val index = match.index
                val instruction = match.instruction
                val register = instruction.registersUsed[0]

                method.addInstructions(
                    index,
                    "invoke-static { v$register }, $GENERAL_CLASS_DESCRIPTOR->fixVideoActionBar($EXTENSION_CONFIG_INFO_INTERFACE)V"
                )

                instruction.getReference<FieldReference>()!!.type
            }

            getConfigInfoFingerprint(configInfoClass).let {
                it.classDef.apply {
                    interfaces.add(EXTENSION_CONFIG_INFO_INTERFACE)

                    mapOf(
                        0 to "patch_setColdConfigData",
                        1 to "patch_setColdHashData",
                    ).forEach { (matchIndex, interfaceMethodName) ->
                        val coldDataField = it.instructionMatches[matchIndex]
                            .instruction.getReference<FieldReference>()!!

                        methods.add(
                            ImmutableMethod(
                                type,
                                interfaceMethodName,
                                listOf(ImmutableMethodParameter("Ljava/lang/String;", null, null)),
                                "V",
                                AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                                null,
                                null,
                                MutableMethodImplementation(3),
                            ).toMutable().apply {
                                addInstructions(
                                    0,
                                    """
                                        iput-object p1, p0, $coldDataField
                                        return-void
                                    """
                                )
                            }
                        )
                    }
                }
            }

            listOf(
                ModernRelateVideoOverlayFingerprint,
                RelateVideoOverlayLayoutParamFingerprint
            ).forEach { fingerprint ->
                fingerprint.clearMatch()
                fingerprint.matchAll().forEach {
                    it.method.insertLiteralOverride(
                        it.instructionMatches.first().index,
                        "$GENERAL_CLASS_DESCRIPTOR->fixRelatedVideoOverlay(Z)Z"
                    )
                }
            }
        }
    }
}

@Suppress("unused")
val actionButtonsPatch = bytecodePatch(
    HIDE_ACTION_BUTTONS.title,
    HIDE_ACTION_BUTTONS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        lazilyConvertedElementHookPatch,
        fixProtoLibraryPatch,
        restoreOldVideoActionBarPatch,
        videoInformationPatch,
        videoIdPatch,
        authHookPatch,
        hypeButtonIconPatch,
    )

    execute {
        addLithoFilter(FILTER_CLASS_DESCRIPTOR)
        hookElementList("$FILTER_CLASS_DESCRIPTOR->onLazilyConvertedElementLoaded")

        // region patch for hide action buttons by index

        hookPlayerResponseVideoId("$ACTION_BUTTONS_CLASS_DESCRIPTOR->fetchRequest(Ljava/lang/String;Z)V")
        hookElementList("$ACTION_BUTTONS_CLASS_DESCRIPTOR->hideActionButtonByIndex")

        // endregion

        WatchNextResponseParserFingerprint.let {
            it.clearMatch()
            it.method.apply {
                val index = it.instructionMatches[5].index
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $FILTER_CLASS_DESCRIPTOR->" +
                        "onSingleColumnWatchNextResultsLoaded(Lcom/google/protobuf/MessageLite;)V"
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: HIDE_ACTION_BUTTONS"
            ),
            HIDE_ACTION_BUTTONS
        )

        // endregion

    }
}
