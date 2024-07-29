package app.revanced.patches.youtube.player.overlaybuttons

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.player.overlaybuttons.fingerprints.HookDownloadPlaylistButtonFingerprint
import app.revanced.patches.youtube.player.overlaybuttons.fingerprints.AccessibilityOfflineButtonSyncFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
object HookPlaylistDownloadButtonPatch : BaseBytecodePatch(
    name = "Hook playlist download button",
    description = "Adds options to hook the playlist download button.",
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AccessibilityOfflineButtonSyncFingerprint,
        HookDownloadPlaylistButtonFingerprint
    )
) {
    private const val INTEGRATIONS_DOWNLOAD_PLAYLIST_BUTTON_CLASS_DESCRIPTOR =
        "$MISC_PATH/DownloadPlaylistButton;"

    override fun execute(context: BytecodeContext) {
        // region Force shown playlist button
        AccessibilityOfflineButtonSyncFingerprint.resultOrThrow().let {
            val targetMethod = it.mutableClass.methods.first { method ->
                method.parameters == listOf("Ljava/lang/Boolean;")
                        && method.returnType == "V"
            }
            targetMethod.apply {
                addInstructions(
                    2,
                    """
                    invoke-static {}, $INTEGRATIONS_DOWNLOAD_PLAYLIST_BUTTON_CLASS_DESCRIPTOR->isPlaylistDownloadButtonHooked()Z
                    move-result p1
                    """.trimIndent()
                )
            }
        }
        // endregion

        // region Hook Download Playlist Button OnClick method
        HookDownloadPlaylistButtonFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                // region Get the index of the instruction that initializes the onClickListener

                val onClickListenerInitializeIndex = indexOfFirstInstructionOrThrow {
                    val reference = ((this as? ReferenceInstruction)?.reference as? MethodReference)

                    opcode == Opcode.INVOKE_VIRTUAL_RANGE
                            && reference?.parameterTypes?.first() == "Ljava/lang/String;"
                }

                // endregion

                // region Get the class that contains the onClick method

                val onClickListenerInitializeReference =
                    getInstruction<ReferenceInstruction>(onClickListenerInitializeIndex).reference


                val onClickClass = context.findClass(
                    (onClickListenerInitializeReference as MethodReference).returnType
                )!!.mutableClass

                // endregion

                onClickClass.methods.find { method -> method.name == "onClick" }?.apply {
                    // region Get the index of playlist id

                    val insertIndex = implementation!!.instructions.indexOfFirst { instruction ->
                        instruction.opcode == Opcode.INVOKE_STATIC
                                && instruction.getReference<MethodReference>()?.name == "isEmpty"
                    }

                    val insertRegister = getInstruction<Instruction35c>(insertIndex).registerC

                    // endregion

                    addInstructions(
                        insertIndex,
                        """
                        invoke-static {v$insertRegister}, $INTEGRATIONS_DOWNLOAD_PLAYLIST_BUTTON_CLASS_DESCRIPTOR->startPlaylistDownloadActivity(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$insertRegister
                        """.trimIndent()
                    )
                }
            }
        }
        // endregion
        
        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: HOOK_PLAYLIST_DOWNLOAD_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
