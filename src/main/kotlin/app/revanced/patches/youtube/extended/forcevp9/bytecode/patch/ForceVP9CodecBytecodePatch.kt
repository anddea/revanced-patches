package app.revanced.patches.youtube.extended.forcevp9.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.extended.forcevp9.bytecode.fingerprints.*
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.toErrorResult
import app.revanced.shared.fingerprints.LayoutSwitchFingerprint
import app.revanced.shared.util.integrations.Constants.EXTENDED_PATH
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.dexbacked.reference.DexBackedFieldReference
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction

@Name("force-vp9-codec-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class ForceVP9CodecBytecodePatch : BytecodePatch(
    listOf(
        LayoutSwitchFingerprint,
        Vp9PrimaryFingerprint,
        Vp9PropsFingerprint,
        Vp9PropsParentFingerprint,
        Vp9SecondaryFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        LayoutSwitchFingerprint.result?.let { parentResult ->
            arrayOf(
                Vp9PrimaryFingerprint,
                Vp9SecondaryFingerprint
            ).map {
                it.also { it.resolve(context, parentResult.classDef) }.result?.injectOverride() ?: return it.toErrorResult()
            }
        } ?: return LayoutSwitchFingerprint.toErrorResult()

        Vp9PropsParentFingerprint.result?.let { parentResult ->
            Vp9PropsFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.let {
                it.hookProps("MANUFACTURER", "getManufacturer")
                it.hookProps("BRAND", "getBrand")
                it.hookProps("MODEL", "getModel")
            } ?: return Vp9PropsFingerprint.toErrorResult()
        } ?: return Vp9PropsParentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$EXTENDED_PATH/CodecOverridePatch;"

        const val INTEGRATIONS_CLASS_METHOD_REFERENCE =
            "$INTEGRATIONS_CLASS_DESCRIPTOR->shouldForceVP9(Z)Z"

        fun MethodFingerprintResult.injectOverride() {
            with (mutableMethod) {
                val startIndex = scanResult.patternScanResult!!.startIndex
                val endIndex = scanResult.patternScanResult!!.endIndex

                val startRegister = (instruction(startIndex) as OneRegisterInstruction).registerA
                val endRegister = (instruction(endIndex) as OneRegisterInstruction).registerA

                hookOverride(endIndex + 1, endRegister)
                removeInstruction(endIndex)
                hookOverride(startIndex + 1, startRegister)
                removeInstruction(startIndex)
            }
        }

        fun MutableMethod.hookOverride(
            index: Int,
            register: Int
        ) {
            addInstructions(
                index, """
                    invoke-static {v$register}, $INTEGRATIONS_CLASS_METHOD_REFERENCE
                    move-result v$register
                    return v$register
                """
            )
        }

        fun MutableMethod.hookProps(
            descriptor: String,
            fieldName: String
        ) {
            val insertInstructions = implementation!!.instructions
            val targetString = "Landroid/os/Build;->" +
                    descriptor +
                    ":Ljava/lang/String;"

            for ((index, instruction) in insertInstructions.withIndex()) {
                if (instruction.opcode != Opcode.SGET_OBJECT) continue

                val indexString = ((instruction as? ReferenceInstruction)?.reference as? DexBackedFieldReference).toString()

                if (indexString != targetString) continue

                val register = (instruction as OneRegisterInstruction).registerA

                addInstructions(
                    index + 1, """
                        invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR->$fieldName(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                        """
                )
                break
            }
        }
    }

}
