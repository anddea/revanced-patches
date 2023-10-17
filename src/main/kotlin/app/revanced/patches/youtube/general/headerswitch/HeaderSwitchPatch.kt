package app.revanced.patches.youtube.general.headerswitch

import app.revanced.extensions.findMutableMethodOf
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.WordMarkHeader
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.bytecode.isWideLiteralExists
import app.revanced.util.integrations.Constants.GENERAL
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Header switch",
    description = "Add switch to change header.",
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
                "18.37.36"
            ]
        )
    ]
)
@Suppress("unused")
object HeaderSwitchPatch : BytecodePatch() {
    @Suppress("LABEL_NAME_CLASH")
    override fun execute(context: BytecodeContext) {
        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (!method.isWideLiteralExists(WordMarkHeader))
                    return@forEach

                context.proxy(classDef)
                    .mutableClass
                    .findMutableMethodOf(method)
                    .apply {
                        val targetIndex = getWideLiteralIndex(WordMarkHeader)
                        val targetRegister =
                            getInstruction<OneRegisterInstruction>(targetIndex).registerA

                        addInstructions(
                            targetIndex + 1, """
                                invoke-static {v$targetRegister}, $GENERAL->enablePremiumHeader(I)I
                                move-result v$targetRegister
                                """
                        )
                    }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HEADER_SWITCH"
            )
        )

        SettingsPatch.updatePatchStatus("header-switch")

    }
}
