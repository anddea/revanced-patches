package app.revanced.patches.youtube.utils.navbarindex

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patches.youtube.utils.fingerprints.OnBackPressedFingerprint
import app.revanced.patches.youtube.utils.navbarindex.fingerprints.MobileTopBarButtonOnClickFingerprint
import app.revanced.patches.youtube.utils.navbarindex.fingerprints.NavButtonOnClickFingerprint
import app.revanced.patches.youtube.utils.navbarindex.fingerprints.NavButtonOnClickLegacyFingerprint
import app.revanced.patches.youtube.utils.navbarindex.fingerprints.OnResumeFragmentsFingerprints
import app.revanced.patches.youtube.utils.navbarindex.fingerprints.SettingsActivityOnBackPressedFingerprint
import app.revanced.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object NavBarIndexHookPatch : BytecodePatch(
    setOf(
        MobileTopBarButtonOnClickFingerprint,
        NavButtonOnClickFingerprint,
        NavButtonOnClickLegacyFingerprint,
        OnBackPressedFingerprint,
        OnResumeFragmentsFingerprints,
        SettingsActivityOnBackPressedFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        val navButtonOnClickFingerprintResult =
            NavButtonOnClickFingerprint.result
                ?: NavButtonOnClickLegacyFingerprint.result
                ?: throw NavButtonOnClickFingerprint.exception
        /**
         * Change NavBar Index value according to selected Tab
         */
        navButtonOnClickFingerprintResult.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex - 1
                val targetString =
                    getInstruction<BuilderInstruction35c>(insertIndex - 2).reference.toString()
                if (!targetString.endsWith("Ljava/util/ArrayList;->indexOf(Ljava/lang/Object;)I"))
                    throw PatchException("Reference not found: $targetString")
                val indexRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$indexRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->setCurrentNavBarIndex(I)V"
                )
            }
        }

        /**
         *  Set NavBar index to last index on back press
         */
        mapOf(
            OnBackPressedFingerprint to 0,
            OnResumeFragmentsFingerprints to 1,
            SettingsActivityOnBackPressedFingerprint to 0
        ).forEach { (fingerprint, index) ->
            fingerprint.setLastNavBarIndexHook(index)
        }

        /**
         * Set Navbar index to zero on clicking MobileTopBarButton
         * May be you want to switch to Incognito mode while in Library Tab
         */
        MobileTopBarButtonOnClickFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        const/4 v0, 0x0
                        invoke-static {v0}, $INTEGRATIONS_CLASS_DESCRIPTOR->setCurrentNavBarIndex(I)V
                        """
                )
            }
        } ?: throw MobileTopBarButtonOnClickFingerprint.exception
    }

    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$UTILS_PATH/NavBarIndexPatch;"

    /**
     * Hook setLastNavBarIndex method
     *
     * @param insertIndex target index at which we want to inject the method call
     */
    private fun MethodFingerprint.setLastNavBarIndexHook(insertIndex: Int) {
        result?.let {
            it.mutableMethod.apply {
                addInstruction(
                    insertIndex,
                    "invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->setLastNavBarIndex()V"
                )
            }
        } ?: throw exception
    }

    internal fun MethodFingerprint.injectIndex(index: Int) {
        result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        const/4 v0, 0x$index
                        invoke-static {v0}, $INTEGRATIONS_CLASS_DESCRIPTOR->setCurrentNavBarIndex(I)V
                        """
                )
            }
        } ?: throw exception
    }
}