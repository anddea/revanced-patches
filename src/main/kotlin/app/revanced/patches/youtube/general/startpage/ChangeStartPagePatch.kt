package app.revanced.patches.youtube.general.startpage

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.general.startpage.fingerprints.BrowseIdFingerprint
import app.revanced.patches.youtube.general.startpage.fingerprints.IntentActionFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

@Suppress("unused")
object ChangeStartPagePatch : BaseBytecodePatch(
    name = "Change start page",
    description = "Adds an option to set which page the app opens in instead of the homepage.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        BrowseIdFingerprint,
        IntentActionFingerprint,
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR = "$GENERAL_PATH/ChangeStartPagePatch;"

    override fun execute(context: BytecodeContext) {

        // Hook browseId.
        BrowseIdFingerprint.resultOrThrow().mutableMethod.apply {
            val browseIdIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.CONST_STRING &&
                        getReference<StringReference>()?.string == "FEwhat_to_watch"
            }
            val browseIdRegister = getInstruction<OneRegisterInstruction>(browseIdIndex).registerA

            addInstructions(
                browseIdIndex + 1, """
                    invoke-static { v$browseIdRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->overrideBrowseId(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$browseIdRegister
                    """
            )
        }

        // There is no browseId assigned to Shorts and Search.
        // Just hook the Intent action.
        IntentActionFingerprint.resultOrThrow().mutableMethod.addInstruction(
            0,
            "invoke-static { p1 }, $INTEGRATIONS_CLASS_DESCRIPTOR->overrideIntentAction(Landroid/content/Intent;)V"
        )

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: CHANGE_START_PAGE"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
