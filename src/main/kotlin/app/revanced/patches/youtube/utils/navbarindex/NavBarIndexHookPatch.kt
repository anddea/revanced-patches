package app.revanced.patches.youtube.utils.navbarindex

import app.revanced.extensions.exception
import app.revanced.extensions.findMutableMethodOf
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.fingerprints.OnBackPressedFingerprint
import app.revanced.patches.youtube.utils.navbarindex.fingerprints.DefaultTabsBarFingerprint
import app.revanced.patches.youtube.utils.navbarindex.fingerprints.MobileTopBarButtonOnClickFingerprint
import app.revanced.patches.youtube.utils.navbarindex.fingerprints.OnResumeFragmentsFingerprints
import app.revanced.patches.youtube.utils.navbarindex.fingerprints.SettingsActivityOnBackPressedFingerprint
import app.revanced.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.Reference

@Suppress("unused")
object NavBarIndexHookPatch : BytecodePatch(
    setOf(
        DefaultTabsBarFingerprint,
        MobileTopBarButtonOnClickFingerprint,
        OnBackPressedFingerprint,
        OnResumeFragmentsFingerprints,
        SettingsActivityOnBackPressedFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Change NavBar Index value according to selected Tab
         */
        DefaultTabsBarFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val setTabIndexReference =
                    getInstruction<ReferenceInstruction>(targetIndex).reference

                val (onClickMethod, insertIndex) = getOnClickMethod(context, setTabIndexReference)

                onClickMethod.apply {
                    val indexRegister =
                        getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                    addInstruction(
                        insertIndex,
                        "invoke-static {v$indexRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->setCurrentNavBarIndex(I)V"
                    )
                }
            }
        } ?: throw DefaultTabsBarFingerprint.exception

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

    private fun getOnClickMethod(
        context: BytecodeContext,
        targetReference: Reference
    ): Pair<MutableMethod, Int> {
        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                if (method.name == "onClick") {
                    method.implementation?.instructions?.forEachIndexed { index, instruction ->
                        if (instruction.opcode != Opcode.INVOKE_VIRTUAL)
                            return@forEachIndexed
                        if ((instruction as ReferenceInstruction).reference != targetReference)
                            return@forEachIndexed

                        val targetMethod = context.proxy(classDef)
                            .mutableClass
                            .findMutableMethodOf(method)

                        return Pair(targetMethod, index)
                    }
                }
            }
        }
        throw PatchException("OnClickMethod not found!")
    }

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