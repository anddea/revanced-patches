/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.autocaptions

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.util.forEachInlinedFeatureFlagSite
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.captions.baseAutoCaptionsPatch
import app.morphe.patches.shared.captions.subtitleTrackFingerprint
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.patch.PatchList.DISABLE_FORCED_AUTO_CAPTIONS
import app.morphe.patches.youtube.utils.playservice.is_20_26_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/shared/patches/AutoCaptionsPatch;"

/**
 * YouTube 20.26+ feature flag that enables automatic captions while muted.
 */
private const val NO_VOLUME_CAPTIONS_FEATURE_FLAG = 45692436L

internal object NoVolumeCaptionsFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    filters = listOf(
        literal(NO_VOLUME_CAPTIONS_FEATURE_FLAG),
    ),
)

@Suppress("unused")
val autoCaptionsPatch = bytecodePatch(
    DISABLE_FORCED_AUTO_CAPTIONS.title,
    DISABLE_FORCED_AUTO_CAPTIONS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        baseAutoCaptionsPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {

        if (is_20_26_or_greater) {
            // The base patch keeps the legacy Boolean setting for older YouTube versions and Music.
            // YouTube 20.26+ replaces only that injected call with the four-state setting.
            subtitleTrackFingerprint.methodOrThrow().apply {
                val legacyCallIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.toString() ==
                        "$EXTENSION_CLASS_DESCRIPTOR->disableAutoCaptions()Z"
                }
                replaceInstruction(
                    legacyCallIndex,
                    "invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->disableAutoCaptionsByStyle()Z",
                )
            }

            // modified by lavinhoque33, 2026-10-04
            // 21.39 inlines the flag getter into its callers, so the dedicated getter method
            // no longer exists. Fall back to overriding every inlined flag read.
            val flagGetter = runCatching { NoVolumeCaptionsFeatureFlagFingerprint.match() }.getOrNull()
            if (flagGetter != null) {
                flagGetter.method.addInstructions(
                    0,
                    """
                        invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->disableMuteAutoCaptions()Z
                        move-result v0
                        return v0
                        nop
                    """,
                )
            } else {
                val patched = forEachInlinedFeatureFlagSite(NO_VOLUME_CAPTIONS_FEATURE_FLAG) { literalIndex ->
                    val moveResultIndex = indexOfFirstInstructionOrThrow(literalIndex, Opcode.MOVE_RESULT)
                    val register = getInstruction<OneRegisterInstruction>(moveResultIndex).registerA
                    addInstructions(
                        moveResultIndex + 1,
                        """
                            invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->disableMuteAutoCaptions()Z
                            move-result v$register
                        """,
                    )
                }
                if (patched == 0) {
                    throw PatchException("NoVolumeCaptionsFeatureFlag: no flag read found")
                }
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                if (is_20_26_or_greater) {
                    "SETTINGS: AUTO_CAPTIONS_STYLE"
                } else {
                    "SETTINGS: DISABLE_AUTO_CAPTIONS"
                },
            ),
            DISABLE_FORCED_AUTO_CAPTIONS
        )

        // endregion

    }
}
