package app.revanced.patches.shared.mainactivity

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import kotlin.properties.Delegates

lateinit var mainActivityMutableClass: MutableClass
    private set
lateinit var onConfigurationChangedMethod: MutableMethod
    private set
lateinit var onCreateMethod: MutableMethod
    private set

private lateinit var constructorMethod: MutableMethod
private lateinit var onBackPressedMethod: MutableMethod

private var constructorMethodIndex by Delegates.notNull<Int>()
private var onBackPressedMethodIndex by Delegates.notNull<Int>()

fun baseMainActivityResolvePatch(
    mainActivityOnCreateFingerprint: Pair<String, Fingerprint>,
) = bytecodePatch(
    description = "baseMainActivityResolvePatch"
) {
    execute {
        onCreateMethod = mainActivityOnCreateFingerprint.methodOrThrow()
        mainActivityMutableClass = mainActivityOnCreateFingerprint.mutableClassOrThrow()

        // set constructor method
        constructorMethod = getMainActivityMethod("<init>")
        constructorMethodIndex = constructorMethod.implementation!!.instructions.lastIndex

        // set onBackPressed method
        onBackPressedMethod = getMainActivityMethod("onBackPressed")
        onBackPressedMethodIndex =
            onBackPressedMethod.indexOfFirstInstructionOrThrow(Opcode.RETURN_VOID)

        // set onConfigurationChanged method
        onConfigurationChangedMethod = getMainActivityMethod("onConfigurationChanged")
    }
}

internal fun injectConstructorMethodCall(classDescriptor: String, methodDescriptor: String) =
    constructorMethod.injectMethodCall(
        classDescriptor,
        methodDescriptor,
        constructorMethodIndex
    )

internal fun injectOnBackPressedMethodCall(classDescriptor: String, methodDescriptor: String) =
    onBackPressedMethod.injectMethodCall(
        classDescriptor,
        methodDescriptor,
        onBackPressedMethodIndex
    )

internal fun injectOnCreateMethodCall(classDescriptor: String, methodDescriptor: String) =
    onCreateMethod.injectMethodCall(classDescriptor, methodDescriptor)

internal fun getMainActivityMethod(methodDescriptor: String) =
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
