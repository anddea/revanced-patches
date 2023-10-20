package app.revanced.patches.youtube.general.accountmenu

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.general.accountmenu.fingerprints.AccountMenuFingerprint
import app.revanced.patches.youtube.general.accountmenu.fingerprints.AccountMenuParentFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch(
    name = "Hide account menu",
    description = "Hide account menu elements.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.24.37",
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
                "18.40.34"
            ]
        )
    ]
)
@Suppress("unused")
object AccountMenuPatch : BytecodePatch(
    setOf(AccountMenuParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        AccountMenuParentFingerprint.result?.let { parentResult ->
            AccountMenuFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex + 2
                    val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

                    addInstruction(
                        targetIndex,
                        "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                                "$GENERAL->hideAccountMenu(Landroid/view/View;Ljava/lang/CharSequence;)V"
                    )
                }
            } ?: throw AccountMenuFingerprint.exception
        } ?: throw AccountMenuParentFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_ACCOUNT_MENU"
            )
        )

        SettingsPatch.updatePatchStatus("Hide account menu")

    }
}