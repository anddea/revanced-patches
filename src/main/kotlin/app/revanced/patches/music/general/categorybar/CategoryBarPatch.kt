package app.revanced.patches.music.general.categorybar

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.general.categorybar.fingerprints.ChipCloudFingerprint
import app.revanced.patches.music.utils.integrations.Constants.GENERAL
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Hide category bar",
    description = "Adds an option to hide the category bar.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ]
)
@Suppress("unused")
object CategoryBarPatch : BytecodePatch(
    setOf(ChipCloudFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        ChipCloudFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static { v$targetRegister }, $GENERAL->hideCategoryBar(Landroid/view/View;)V"
                )
            }
        } ?: throw ChipCloudFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_hide_category_bar",
            "false"
        )

    }
}
