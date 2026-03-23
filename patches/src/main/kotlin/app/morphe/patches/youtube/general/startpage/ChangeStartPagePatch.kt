package app.morphe.patches.youtube.general.startpage

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.CHANGE_START_PAGE
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "$GENERAL_PATH/ChangeStartPagePatch;"

val changeStartPagePatch = bytecodePatch(
    CHANGE_START_PAGE.title,
    CHANGE_START_PAGE.summary,
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
    )

    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        // Hook browseId.
        BrowseIdFingerprint.let {
            it.method.apply {
                val browseIdIndex = it.instructionMatches.first().index
                val browseIdRegister = getInstruction<OneRegisterInstruction>(browseIdIndex).registerA

                addInstructions(
                    browseIdIndex + 1,
                    """
                        invoke-static { v$browseIdRegister }, $EXTENSION_CLASS_DESCRIPTOR->overrideBrowseId(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$browseIdRegister
                    """
                )
            }
        }

        // There is no browserId assigned to Shorts and Search.
        // Just hook the Intent action.
        IntentActionFingerprint.method.addInstruction(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->overrideIntentAction(Landroid/content/Intent;)V",
        )
        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: CHANGE_START_PAGE"
            ),
            CHANGE_START_PAGE
        )

        // endregion
    }
}
