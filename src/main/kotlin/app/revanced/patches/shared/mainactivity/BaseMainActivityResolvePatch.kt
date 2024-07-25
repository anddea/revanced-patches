package app.revanced.patches.shared.mainactivity

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.util.getTargetIndexOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import kotlin.properties.Delegates

abstract class BaseMainActivityResolvePatch(
    private val mainActivityOnCreateFingerprint: MethodFingerprint
) : BytecodePatch(
    setOf(mainActivityOnCreateFingerprint)
) {
    lateinit var mainActivityMutableClass: MutableClass
    lateinit var onConfigurationChangedMethod: MutableMethod

    private lateinit var constructorMethod: MutableMethod
    private lateinit var onBackPressedMethod: MutableMethod
    private lateinit var onCreateMethod: MutableMethod

    private var constructorMethodIndex by Delegates.notNull<Int>()
    private var onBackPressedMethodIndex by Delegates.notNull<Int>()

    override fun execute(context: BytecodeContext) {
        val mainActivityResult = mainActivityOnCreateFingerprint.resultOrThrow()
        onCreateMethod = mainActivityResult.mutableMethod
        mainActivityMutableClass = mainActivityResult.mutableClass

        // set constructor method
        constructorMethod = getMethod("<init>")
        constructorMethodIndex = constructorMethod.implementation!!.instructions.size - 1

        // set onBackPressed method
        onBackPressedMethod = getMethod("onBackPressed")
        onBackPressedMethodIndex = onBackPressedMethod.getTargetIndexOrThrow(Opcode.RETURN_VOID)

        // set onConfigurationChanged method
        onConfigurationChangedMethod = getMethod("onConfigurationChanged")
    }

    fun injectConstructorMethodCall(classDescriptor: String, methodDescriptor: String) =
        constructorMethod.injectMethodCall(
            classDescriptor,
            methodDescriptor,
            constructorMethodIndex
        )

    fun injectOnBackPressedMethodCall(classDescriptor: String, methodDescriptor: String) =
        onBackPressedMethod.injectMethodCall(
            classDescriptor,
            methodDescriptor,
            onBackPressedMethodIndex
        )

    fun injectOnCreateMethodCall(classDescriptor: String, methodDescriptor: String) =
        onCreateMethod.injectMethodCall(classDescriptor, methodDescriptor)

    private fun getMethod(methodDescriptor: String) =
        mainActivityMutableClass.methods.find { method -> method.name == methodDescriptor }
            ?: throw PatchException("Could not find $methodDescriptor")

    private fun MutableMethod.injectMethodCall(
        classDescriptor: String,
        methodDescriptor: String
    ) = injectMethodCall(classDescriptor, methodDescriptor, 0)

    private fun MutableMethod.injectMethodCall(
        classDescriptor: String,
        methodDescriptor: String,
        insertIndex: Int
    ) = addInstruction(
        insertIndex,
        "invoke-static/range {p0 .. p0}, $classDescriptor->$methodDescriptor(Landroid/app/Activity;)V"
    )
}