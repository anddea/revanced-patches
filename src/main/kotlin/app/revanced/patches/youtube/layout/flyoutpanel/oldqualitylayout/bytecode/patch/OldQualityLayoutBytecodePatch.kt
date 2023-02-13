package app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.bytecode.fingerprints.QualityMenuViewInflateFingerprint
import app.revanced.util.integrations.Constants.FLYOUTPANEL_LAYOUT
import org.jf.dexlib2.iface.instruction.FiveRegisterInstruction

@Name("enable-old-quality-layout-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class OldQualityLayoutBytecodePatch : BytecodePatch(
    listOf(QualityMenuViewInflateFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        QualityMenuViewInflateFingerprint.result?.mutableMethod?.let {
            with (it.implementation!!.instructions) {
                val insertIndex = this.size - 1 - 1
                val register = (this[insertIndex] as FiveRegisterInstruction).registerC

                it.addInstructions(
                    insertIndex, // insert the integrations instructions right before the listener
                    "invoke-static { v$register }, $FLYOUTPANEL_LAYOUT->enableOldQualityMenu(Landroid/widget/ListView;)V"
                )
            }
        } ?: return QualityMenuViewInflateFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
