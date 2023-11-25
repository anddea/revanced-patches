package app.revanced.patches.music.flyoutpanel.compactdialog

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.flyoutpanel.compactdialog.fingerprints.DialogSolidFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_FLYOUT

@Patch(
    name = "Enable compact dialog",
    description = "Enable compact dialog on phone.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.27.54",
                "6.28.52"
            ]
        )
    ]
)
@Suppress("unused")
object CompactDialogPatch : BytecodePatch(
    setOf(DialogSolidFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        DialogSolidFingerprint.result?.let {
            with(
                context
                    .toMethodWalker(it.method)
                    .nextMethod(it.scanResult.patternScanResult!!.endIndex, true)
                    .getMethod() as MutableMethod
            ) {
                addInstructions(
                    2, """
                        invoke-static {p0}, $MUSIC_FLYOUT->enableCompactDialog(I)I
                        move-result p0
                        """
                )
            }
        } ?: throw DialogSolidFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.FLYOUT,
            "revanced_enable_compact_dialog",
            "true"
        )

    }
}
