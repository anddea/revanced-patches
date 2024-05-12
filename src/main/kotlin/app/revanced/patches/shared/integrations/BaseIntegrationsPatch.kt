package app.revanced.patches.shared.integrations

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patches.shared.integrations.BaseIntegrationsPatch.IntegrationsFingerprint.IRegisterResolver
import app.revanced.patches.shared.integrations.Constants.INTEGRATIONS_UTILS_CLASS_DESCRIPTOR
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method

abstract class BaseIntegrationsPatch(
    private val hooks: Set<IntegrationsFingerprint>,
) : BytecodePatch(hooks) {

    override fun execute(context: BytecodeContext) {
        if (context.findClass(INTEGRATIONS_UTILS_CLASS_DESCRIPTOR) == null) {
            throw PatchException(
                "Integrations have not been merged yet. This patch can not succeed without merging the integrations.",
            )
        }

        hooks.forEach { hook ->
            hook.invoke(INTEGRATIONS_UTILS_CLASS_DESCRIPTOR)
        }
    }

    /**
     * [MethodFingerprint] for integrations.
     *
     * @param contextRegisterResolver A [IRegisterResolver] to get the register.
     * @see MethodFingerprint
     */
    abstract class IntegrationsFingerprint(
        returnType: String? = null,
        accessFlags: Int? = null,
        parameters: Iterable<String>? = null,
        opcodes: Iterable<Opcode?>? = null,
        strings: Iterable<String>? = null,
        customFingerprint: ((methodDef: Method, classDef: ClassDef) -> Boolean)? = null,
        private val insertIndexResolver: ((Method) -> Int) = object : IHookInsertIndexResolver {},
        private val contextRegisterResolver: (Method) -> String = object : IRegisterResolver {},
    ) : MethodFingerprint(
        returnType,
        accessFlags,
        parameters,
        opcodes,
        strings,
        customFingerprint,
    ) {

        fun invoke(integrationsDescriptor: String) {
            resultOrThrow().mutableMethod.let { method ->
                val insertIndex = insertIndexResolver(method)
                val contextRegister = contextRegisterResolver(method)

                method.addInstruction(
                    insertIndex,
                    "invoke-static/range { $contextRegister .. $contextRegister }, " +
                            "$integrationsDescriptor->setContext(Landroid/content/Context;)V",
                )
            }
        }

        interface IHookInsertIndexResolver : (Method) -> Int {
            override operator fun invoke(method: Method) = 0
        }

        interface IRegisterResolver : (Method) -> String {
            override operator fun invoke(method: Method) = "p0"
        }
    }
}