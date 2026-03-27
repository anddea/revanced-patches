/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.interaction.reload

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.mainactivity.mainActivityFingerprint
import app.morphe.patches.youtube.utils.playercontrols.addTopControl
import app.morphe.patches.youtube.utils.playercontrols.injectControl
import app.morphe.patches.youtube.utils.playercontrols.playerControlsPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.util.MethodUtil

private val reloadVideoResourcePatch = resourcePatch {
    dependsOn(
        settingsPatch,
        playerControlsPatch,
    )

    execute {
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "PREFERENCE_SCREENS: PLAYER_BUTTONS",
                "SETTINGS: RELOAD_VIDEO",
            )
        )

        copyResources(
            "youtube/reloadbutton/default",
            ResourceGroup(
                resourceDirectoryName = "drawable",
                "morphe_reload_video.xml",
            ),
        )
    }
}

private const val EXTENSION_BUTTON_DESCRIPTOR =
    "Lapp/morphe/extension/youtube/videoplayer/ReloadVideoButton;"

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/youtube/patches/ReloadVideoPatch;"

private const val EXTENSION_PLAYER_INTERFACE =
    "Lapp/morphe/extension/youtube/patches/ReloadVideoPatch\$PlayerInterface;"

@Suppress("unused")
val reloadVideoPatch = bytecodePatch(
    name = "Reload video",
    description = "Adds an option to display a button in the video player to reload the current video.",
) {
    dependsOn(
        reloadVideoResourcePatch,
        playerControlsPatch,
        videoInformationPatch,
    )

    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        injectControl(EXTENSION_BUTTON_DESCRIPTOR)

        // Main activity is used to launch downloader intent.
        mainActivityFingerprint.methodOrThrow().addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->setMainActivity(Landroid/app/Activity;)V"
        )

        val dismissPlayerInnerMethod = MiniAppOpenYtContentCommandEndpointFingerprint
            .instructionMatches[2]
            .getInstruction<ReferenceInstruction>()
            .getReference<MethodReference>()!!

        mutableClassDefBy(dismissPlayerInnerMethod.definingClass).apply {
            // Add interface and helper methods to allow extension code to call obfuscated methods.
            interfaces.add(EXTENSION_PLAYER_INTERFACE)
            // Add methods to access obfuscated player methods.
            methods.add(
                ImmutableMethod(
                    type,
                    "patch_dismissPlayer",
                    listOf(),
                    "V",
                    AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                    null,
                    null,
                    MutableMethodImplementation(2),
                ).toMutable().apply {
                    addInstructions(
                        0,
                        """
                            invoke-virtual { p0 }, $dismissPlayerInnerMethod
                            return-void
                        """
                    )
                }
            )

            methods.single { method ->
                MethodUtil.isConstructor(method)
            }.apply {
                val index = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN_VOID)

                addInstruction(
                    index,
                    "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->initialize($EXTENSION_PLAYER_INTERFACE)V"
                )
            }
        }
    }

    finalize {
        addTopControl(
            "youtube/reloadbutton/shared",
            "@+id/morphe_reload_video_button",
            "@+id/morphe_reload_video_button"
        )
    }
}
