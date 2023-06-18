package app.revanced.patches.youtube.layout.general.accountmenu.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.accountmenu.fingerprints.AccountMenuFingerprint
import app.revanced.patches.youtube.layout.general.accountmenu.fingerprints.AccountMenuParentFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("hide-account-menu")
@Description("Hide account menu elements.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class AccountMenuPatch : BytecodePatch(
    listOf(AccountMenuParentFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        AccountMenuParentFingerprint.result?.let { parentResult ->
            AccountMenuFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex + 1
                    val register = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstruction(
                        targetIndex + 1,
                        "invoke-static {v$register}, $GENERAL->hideAccountMenu(Landroid/text/Spanned;)V"
                    )
                }
            } ?: return AccountMenuFingerprint.toErrorResult()

            parentResult.mutableMethod.apply {
                val endIndex = parentResult.scanResult.patternScanResult!!.endIndex
                val register = getInstruction<OneRegisterInstruction>(endIndex).registerA

                addInstruction(
                    endIndex + 1,
                    "sput-object v$register, $GENERAL->compactLink:Landroid/view/View;"
                )
            }
        } ?: return AccountMenuParentFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_ACCOUNT_MENU"
            )
        )

        SettingsPatch.updatePatchStatus("hide-account-menu")

        return PatchResultSuccess()
    }
}