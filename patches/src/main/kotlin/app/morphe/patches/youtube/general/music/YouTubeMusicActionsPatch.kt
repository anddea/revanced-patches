package app.morphe.patches.youtube.general.music

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.HOOK_YOUTUBE_MUSIC_ACTIONS
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/YouTubeMusicActionsPatch;"

@Suppress("unused")
val youtubeMusicActionsPatch = bytecodePatch(
    HOOK_YOUTUBE_MUSIC_ACTIONS.title,
    HOOK_YOUTUBE_MUSIC_ACTIONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        appDeepLinkFingerprint.matchOrThrow().let {
            it.method.apply {
                val packageNameIndex = it.instructionMatches.first().index
                val packageNameField =
                    getInstruction<ReferenceInstruction>(packageNameIndex).reference.toString()

                implementation!!.instructions
                    .withIndex()
                    .filter { (_, instruction) ->
                        instruction.opcode == Opcode.IGET_OBJECT &&
                                instruction.getReference<FieldReference>()
                                    ?.toString() == packageNameField
                    }
                    .map { (index, _) -> index }
                    .reversed()
                    .forEach { index ->
                        val register = getInstruction<TwoRegisterInstruction>(index).registerA

                        addInstructions(
                            index + 1, """
                                invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->overridePackageName(Ljava/lang/String;)Ljava/lang/String;
                                move-result-object v$register
                                """
                        )
                    }
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: HOOK_BUTTONS",
                "SETTINGS: HOOK_YOUTUBE_MUSIC_ACTIONS"
            ),
            HOOK_YOUTUBE_MUSIC_ACTIONS
        )

        // endregion

    }
}
