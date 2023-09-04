package app.revanced.patches.youtube.player.filmstripoverlay.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.filmstripoverlay.fingerprints.FilmStripOverlayConfigFingerprint
import app.revanced.patches.youtube.player.filmstripoverlay.fingerprints.FilmStripOverlayInteractionFingerprint
import app.revanced.patches.youtube.player.filmstripoverlay.fingerprints.FilmStripOverlayParentFingerprint
import app.revanced.patches.youtube.player.filmstripoverlay.fingerprints.FilmStripOverlayPreviewFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch
@Name("Hide filmstrip overlay")
@Description("Hide filmstrip overlay on swipe controls.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class HideFilmstripOverlayPatch : BytecodePatch(
    listOf(
        FilmStripOverlayParentFingerprint,
        YouTubeControlsOverlayFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        FilmStripOverlayParentFingerprint.result?.classDef?.let { classDef ->
            arrayOf(
                FilmStripOverlayConfigFingerprint,
                FilmStripOverlayInteractionFingerprint,
                FilmStripOverlayPreviewFingerprint
            ).forEach { fingerprint ->
                fingerprint.also {
                    it.resolve(
                        context,
                        classDef
                    )
                }.result?.mutableMethod?.injectHook()
                    ?: throw fingerprint.exception
            }
        } ?: throw FilmStripOverlayParentFingerprint.exception

        YouTubeControlsOverlayFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = getIndex("bringChildToFront") + 1
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                val setOnClickListenerIndex = getIndex("setOnClickListener")
                val jumpIndex = setOnClickListenerIndex + 3

                val initialIndex = setOnClickListenerIndex - 1

                if (SettingsPatch.upward1828) {
                    for (index in insertIndex..initialIndex) {
                        if (getInstruction(index).opcode != Opcode.CONST_16 &&
                            getInstruction(index).opcode != Opcode.CONST_4 &&
                            getInstruction(index).opcode != Opcode.CONST
                        )
                            continue

                        val register = getInstruction<OneRegisterInstruction>(index).registerA
                        val value =
                            getInstruction<WideLiteralInstruction>(index).wideLiteral.toInt()

                        val line =
                            when (getInstruction(index).opcode) {
                                Opcode.CONST_16 -> """
                                const/16 v$register, $value
                                
                                """.trimIndent()

                                Opcode.CONST_4 -> """
                                const/4 v$register, $value
                                
                                """.trimIndent()

                                Opcode.CONST -> """
                                const v$register, $value
                                
                                """.trimIndent()

                                else -> ""
                            }

                        fixComponent += line
                    }
                } else {
                    val fixRegister =
                        getInstruction<FiveRegisterInstruction>(initialIndex).registerE

                    for (index in initialIndex downTo insertIndex) {
                        if (getInstruction(index).opcode != Opcode.CONST_16) continue

                        val register = getInstruction<OneRegisterInstruction>(index).registerA

                        if (register != fixRegister) continue

                        val fixValue =
                            getInstruction<WideLiteralInstruction>(index).wideLiteral.toInt()
                        fixComponent = "const/16 v$fixRegister, $fixValue"
                        break
                    }
                }

                addInstructionsWithLabels(
                    insertIndex, fixComponent + """
                        invoke-static {}, $PLAYER->hideFilmstripOverlay()Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :hidden
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
            }
        } ?: throw YouTubeControlsOverlayFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: PLAYER_EXPERIMENTAL_FLAGS",
                "SETTINGS: HIDE_FILMSTRIP_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("hide-filmstrip-overlay")

    }

    private companion object {
        var fixComponent: String = ""

        fun MutableMethod.injectHook() {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $PLAYER->hideFilmstripOverlay()Z
                    move-result v0
                    if-eqz v0, :shown
                    const/4 v0, 0x0
                    return v0
                    """, ExternalLabel("shown", getInstruction(0))
            )
        }

        fun MutableMethod.getIndex(methodName: String): Int {
            return implementation!!.instructions.indexOfFirst { instruction ->
                if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@indexOfFirst false

                return@indexOfFirst ((instruction as Instruction35c).reference as MethodReference).name == methodName
            }
        }
    }
}
