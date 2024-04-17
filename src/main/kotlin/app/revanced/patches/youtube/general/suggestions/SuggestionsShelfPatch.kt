package app.revanced.patches.youtube.general.suggestions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.general.suggestions.fingerprints.BreakingNewsFingerprint
import app.revanced.patches.youtube.navigation.navigationbuttons.NavigationBarHookPatch
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.litho.LithoFilterPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Hide suggestions shelf",
    description = "Adds an option to hide the suggestions shelf in feed.",
    dependencies = [
        LithoFilterPatch::class,
        NavigationBarHookPatch::class,
        SettingsPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40",
                "19.08.36",
                "19.09.38",
                "19.10.39",
                "19.11.38"
            ]
        )
    ]
)
@Suppress("unused")
object SuggestionsShelfPatch : BytecodePatch(
    setOf(BreakingNewsFingerprint)
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/SuggestionsShelfFilter;"

    override fun execute(context: BytecodeContext) {

        /**
         * Only used to tablet layout and the old UI components.
         */
        BreakingNewsFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $FILTER_CLASS_DESCRIPTOR->hideBreakingNewsShelf(Landroid/view/View;)V"
                )
            }
        } ?: throw BreakingNewsFingerprint.exception

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)


        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: HIDE_SUGGESTIONS_SHELF"
            )
        )

        SettingsPatch.updatePatchStatus("Hide suggestions shelf")

    }
}
