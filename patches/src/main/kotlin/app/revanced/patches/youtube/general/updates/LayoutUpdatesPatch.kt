package app.revanced.patches.youtube.general.updates

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.patch.PatchList.DISABLE_LAYOUT_UPDATES
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.fingerprint.matchOrThrow

@Suppress("unused")
val layoutUpdatesPatch = bytecodePatch(
    DISABLE_LAYOUT_UPDATES.title,
    DISABLE_LAYOUT_UPDATES.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        cronetHeaderFingerprint.matchOrThrow().let {
            it.method.apply {
                val index = it.stringMatches!!.first().index

                addInstructions(
                    index, """
                        invoke-static {p1, p2}, $GENERAL_CLASS_DESCRIPTOR->disableLayoutUpdates(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p2
                        """
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: DISABLE_LAYOUT_UPDATES"
            ),
            DISABLE_LAYOUT_UPDATES
        )

        // endregion

    }
}
