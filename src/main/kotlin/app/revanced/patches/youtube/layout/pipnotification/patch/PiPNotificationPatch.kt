package app.revanced.patches.youtube.layout.pipnotification.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.layout.pipnotification.fingerprints.PiPNotificationFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getStringIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch
@Name("Disable pip notification")
@Description("Disable pip notification when you first launch pip mode.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class PiPNotificationPatch : BytecodePatch(
    listOf(PiPNotificationFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        PiPNotificationFingerprint.result?.let {
            it.mutableMethod.apply {
                var insertIndex = -1

                val startIndex = it.scanResult.patternScanResult!!.startIndex - 6
                val endIndex = getStringIndex("honeycomb.Shell\$HomeActivity")

                for (index in endIndex downTo startIndex) {
                    if (getInstruction(index).opcode != Opcode.CHECK_CAST) continue

                    val targetReference =
                        getInstruction<ReferenceInstruction>(index).reference.toString()

                    if (targetReference == "Lcom/google/apps/tiktok/account/AccountId;") {
                        insertIndex = index + 1

                        addInstruction(
                            insertIndex,
                            "return-void"
                        )
                    }
                }
                if (insertIndex == -1)
                    throw PatchException("Couldn't find target Index")
            }
        } ?: throw PiPNotificationFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.updatePatchStatus("hide-pip-notification")

    }
}