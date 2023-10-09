package app.revanced.patches.youtube.layout.pipnotification

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.layout.pipnotification.fingerprints.PiPNotificationFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.bytecode.getStringIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch(
    name = "Disable pip notification",
    description = "Disable pip notification when you first launch pip mode.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.22.37",
                "18.23.36",
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40"
            ]
        )
    ]
)
@Suppress("unused")
object PiPNotificationPatch : BytecodePatch(
    setOf(PiPNotificationFingerprint)
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