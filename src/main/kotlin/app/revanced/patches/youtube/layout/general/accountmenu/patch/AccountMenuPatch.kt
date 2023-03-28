package app.revanced.patches.youtube.layout.general.accountmenu.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.accountmenu.fingerprints.*
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL_LAYOUT
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
    listOf(
        AccountMenuParentFingerprint,
        LibraryMenuParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        AccountMenuParentFingerprint.result?.let { parentResult ->
            AccountMenuFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                insert(parentResult, it, "hideAccountMenu", "compactLink")
            } ?: return AccountMenuFingerprint.toErrorResult()
        } ?: return AccountMenuParentFingerprint.toErrorResult()

        LibraryMenuParentFingerprint.result?.let { parentResult ->
            LibraryMenuFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                insert(parentResult, it, "hideLibraryMenu", "libraryList")
            } ?: return LibraryMenuFingerprint.toErrorResult()
        } ?: return LibraryMenuParentFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_LAYOUT_SETTINGS",
                "SETTINGS: HIDE_ACCOUNT_MENU"
            )
        )

        SettingsPatch.updatePatchStatus("hide-account-menu")

        return PatchResultSuccess()
    }
    private companion object {
        fun insert(
            viewResult: MethodFingerprintResult,
            spanResult: MethodFingerprintResult,
            targetMethod: String,
            targetField: String
        ) {
            with (spanResult.mutableMethod) {
                val targetIndex = spanResult.scanResult.patternScanResult!!.startIndex + 1
                val register = (instruction(targetIndex) as OneRegisterInstruction).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$register}, $GENERAL_LAYOUT->$targetMethod(Landroid/text/Spanned;)V"
                )
            }

            with (viewResult.mutableMethod) {
                val endIndex = viewResult.scanResult.patternScanResult!!.endIndex
                val register = (instruction(endIndex) as OneRegisterInstruction).registerA

                addInstruction(
                    endIndex + 1,
                    "sput-object v$register, $GENERAL_LAYOUT->$targetField:Landroid/view/View;"
                )
            }
        }
    }
}
