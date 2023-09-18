package app.revanced.patches.music.navigation.component.patch

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
import app.revanced.patches.music.navigation.component.fingerprints.TabLayoutTextFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_NAVIGATION
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch
@Name("Hide navigation bar component")
@Description("Hides navigation bar components.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@MusicCompatibility
class NavigationBarComponentPatch : BytecodePatch(
    listOf(TabLayoutTextFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        /**
         * Hide navigation labels
         */
        TabLayoutTextFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralIndex(SharedResourceIdPatch.Text1) + 3
                val targetParameter = getInstruction<ReferenceInstruction>(targetIndex).reference
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                if (!targetParameter.toString().endsWith("Landroid/widget/TextView;"))
                    throw PatchException("Method signature parameter did not match: $targetParameter")

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $MUSIC_NAVIGATION->hideNavigationLabel(Landroid/widget/TextView;)V"
                )
            }
        } ?: throw TabLayoutTextFingerprint.exception

        SettingsPatch.contexts.xmlEditor[RESOURCE_FILE_PATH].use { editor ->
            val document = editor.file

            with(document.getElementsByTagName("ImageView").item(0)) {
                if (attributes.getNamedItem(FLAG) != null)
                    return@with

                document.createAttribute(FLAG)
                    .apply { value = "0.5" }
                    .let(attributes::setNamedItem)
            }
        }

        /**
         * Hide navigation bar & buttons
         */
        TabLayoutTextFingerprint.result?.let {
            it.mutableMethod.apply {
                val enumIndex = it.scanResult.patternScanResult!!.startIndex + 3
                val enumRegister = getInstruction<OneRegisterInstruction>(enumIndex).registerA

                val insertIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    instruction.opcode == Opcode.AND_INT_LIT8
                } - 2

                val pivotTabIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == "getVisibility"
                }
                val pivotTabRegister = getInstruction<Instruction35c>(pivotTabIndex).registerC

                addInstruction(
                    pivotTabIndex,
                    "invoke-static {v$pivotTabRegister}, $MUSIC_NAVIGATION->hideNavigationButton(Landroid/view/View;)V"
                )

                addInstruction(
                    insertIndex,
                    "sput-object v$enumRegister, $MUSIC_NAVIGATION->lastPivotTab:Ljava/lang/Enum;"
                )
            }
        } ?: throw TabLayoutTextFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_explore_button",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_home_button",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_library_button",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_bar",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_label",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_samples_button",
            "false"
        )
        SettingsPatch.addMusicPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_upgrade_button",
            "true"
        )
    }

    private companion object {
        const val FLAG = "android:layout_weight"
        const val RESOURCE_FILE_PATH = "res/layout/image_with_text_tab.xml"
    }
}
