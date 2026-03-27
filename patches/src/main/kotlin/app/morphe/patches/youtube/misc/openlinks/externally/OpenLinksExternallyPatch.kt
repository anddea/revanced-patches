package app.morphe.patches.youtube.misc.openlinks.externally

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.transformation.transformInstructionsPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.MISC_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.OPEN_LINKS_EXTERNALLY
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

@Suppress("unused")
val openLinksExternallyPatch = bytecodePatch(
    OPEN_LINKS_EXTERNALLY.title,
    OPEN_LINKS_EXTERNALLY.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        transformInstructionsPatch(
            filterMap = filterMap@{ _, _, instruction, instructionIndex ->
                if (instruction !is ReferenceInstruction) return@filterMap null
                val reference = instruction.reference as? StringReference ?: return@filterMap null

                if (reference.string != "android.support.customtabs.action.CustomTabsService") return@filterMap null

                return@filterMap instructionIndex to (instruction as OneRegisterInstruction).registerA
            },
            transform = { mutableMethod, entry ->
                val (intentStringIndex, register) = entry

                // Hook the intent string.
                mutableMethod.addInstructions(
                    intentStringIndex + 1,
                    """
                        invoke-static {v$register}, $MISC_PATH/OpenLinksExternallyPatch;->openLinksExternally(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                        """,
                )
            },
        ),
        settingsPatch,
    )

    execute {

        // region add settings

        addPreference(
            arrayOf(
                "SETTINGS: OPEN_LINKS_EXTERNALLY"
            ),
            OPEN_LINKS_EXTERNALLY
        )

        // endregion

    }
}
