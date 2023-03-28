package app.revanced.patches.shared.patch.versionspoof

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.fingerprints.ClientInfoFingerprint
import app.revanced.patches.shared.fingerprints.ClientInfoParentFingerprint
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.dexbacked.reference.DexBackedFieldReference
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import kotlin.properties.Delegates

@Name("general-version-spoof")
@Version("0.0.1")
class GeneralVersionSpoofPatch : BytecodePatch(
    listOf(
        ClientInfoParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        ClientInfoParentFingerprint.result?.let { parentResult ->
            ClientInfoFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.let {
                val insertInstructions = it.implementation!!.instructions
                val targetString = "Landroid/os/Build\$VERSION;->RELEASE:Ljava/lang/String;"

                for ((index, instruction) in insertInstructions.withIndex()) {
                    if (instruction.opcode != Opcode.SGET_OBJECT) continue

                    val indexString = ((instruction as? ReferenceInstruction)?.reference as? DexBackedFieldReference).toString()

                    if (indexString != targetString) continue

                    insertMethod = it
                    insertIndex = index - 1
                    targetRegister = (instruction as OneRegisterInstruction).registerA
                    break
                }
                if (insertIndex <= 0) return ClientInfoFingerprint.toErrorResult()
            } ?: return ClientInfoFingerprint.toErrorResult()
        } ?: return ClientInfoParentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    companion object {
        private var insertIndex by Delegates.notNull<Int>()
        private var targetRegister by Delegates.notNull<Int>()
        private lateinit var insertMethod: MutableMethod


        fun injectSpoof(methodDescriptor: String) {
            insertMethod.addInstructions(
                insertIndex, """
                    invoke-static {v$targetRegister}, $methodDescriptor
                    move-result-object v$targetRegister
                """
            )
        }
    }
}