@file:Suppress("unused")

package app.revanced.util

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.fingerprint.MethodFingerprintResult
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableField
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31i
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.util.MethodUtil

const val REGISTER_TEMPLATE_REPLACEMENT: String = "REGISTER_INDEX"

fun MethodFingerprint.resultOrThrow() = result ?: throw exception

/**
 * The [PatchException] of failing to resolve a [MethodFingerprint].
 *
 * @return The [PatchException].
 */
val MethodFingerprint.exception
    get() = PatchException("Failed to resolve ${this.javaClass.simpleName}")

/**
 * Find the [MutableMethod] from a given [Method] in a [MutableClass].
 *
 * @param method The [Method] to find.
 * @return The [MutableMethod].
 */
fun MutableClass.findMutableMethodOf(method: Method) = this.methods.first {
    MethodUtil.methodSignaturesMatch(it, method)
}

/**
 * Apply a transform to all fields of the class.
 *
 * @param transform The transformation function. Accepts a [MutableField] and returns a transformed [MutableField].
 */
fun MutableClass.transformFields(transform: MutableField.() -> MutableField) {
    val transformedFields = fields.map { it.transform() }
    fields.clear()
    fields.addAll(transformedFields)
}

/**
 * Apply a transform to all methods of the class.
 *
 * @param transform The transformation function. Accepts a [MutableMethod] and returns a transformed [MutableMethod].
 */
fun MutableClass.transformMethods(transform: MutableMethod.() -> MutableMethod) {
    val transformedMethods = methods.map { it.transform() }
    methods.clear()
    methods.addAll(transformedMethods)
}

/**
 * Inject a call to a method that hides a view.
 *
 * @param insertIndex The index to insert the call at.
 * @param viewRegister The register of the view to hide.
 * @param classDescriptor The descriptor of the class that contains the method.
 * @param targetMethod The name of the method to call.
 */
fun MutableMethod.injectHideViewCall(
    insertIndex: Int,
    viewRegister: Int,
    classDescriptor: String,
    targetMethod: String
) = addInstruction(
    insertIndex,
    "invoke-static { v$viewRegister }, $classDescriptor->$targetMethod(Landroid/view/View;)V"
)

fun MethodFingerprint.literalInstructionBooleanHook(
    literal: Int,
    descriptor: String
) = literalInstructionBooleanHook(literal.toLong(), descriptor)

fun MethodFingerprint.literalInstructionBooleanHook(
    literal: Long,
    descriptor: String
) {
    resultOrThrow().mutableMethod.apply {
        val literalIndex = getWideLiteralInstructionIndex(literal)
        val targetIndex = getTargetIndex(literalIndex, Opcode.MOVE_RESULT)
        val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

        val smaliInstruction = if (descriptor.endsWith("(Z)Z"))
            "invoke-static {v$targetRegister}, $descriptor"
        else
            "invoke-static {}, $descriptor"

        addInstructions(
            targetIndex + 1, """
                $smaliInstruction
                move-result v$targetRegister
                """
        )
    }
}

fun MethodFingerprint.literalInstructionViewHook(
    literal: Long,
    smaliInstruction: String
) = resultOrThrow().mutableMethod.literalInstructionViewHook(literal, smaliInstruction)

fun MutableMethod.literalInstructionViewHook(
    literal: Long,
    smaliInstruction: String
) {
    val literalIndex = getWideLiteralInstructionIndex(literal)
    val targetIndex = getTargetIndex(literalIndex, Opcode.MOVE_RESULT_OBJECT)
    val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA.toString()

    addInstructions(
        targetIndex + 1,
        smaliInstruction.replace(REGISTER_TEMPLATE_REPLACEMENT, targetRegister)
    )
}

fun BytecodeContext.literalInstructionViewHook(
    literal: Long,
    smaliInstruction: String
) {
    val context = this
    context.classes.forEach { classDef ->
        classDef.methods.forEach { method ->
            method.implementation.apply {
                this?.instructions?.forEachIndexed { _, instruction ->
                    if (instruction.opcode != Opcode.CONST)
                        return@forEachIndexed
                    if ((instruction as Instruction31i).wideLiteral != literal)
                        return@forEachIndexed

                    context.proxy(classDef)
                        .mutableClass
                        .findMutableMethodOf(method)
                        .literalInstructionViewHook(literal, smaliInstruction)
                }
            }
        }
    }
}

fun BytecodeContext.literalInstructionHook(
    literal: Long,
    smaliInstruction: String
) {
    val context = this
    context.classes.forEach { classDef ->
        classDef.methods.forEach { method ->
            method.implementation.apply {
                this?.instructions?.forEachIndexed { _, instruction ->
                    if (instruction.opcode != Opcode.CONST)
                        return@forEachIndexed
                    if ((instruction as Instruction31i).wideLiteral != literal)
                        return@forEachIndexed

                    context.proxy(classDef)
                        .mutableClass
                        .findMutableMethodOf(method).apply {
                            val index = getWideLiteralInstructionIndex(literal)
                            val register = (instruction as OneRegisterInstruction).registerA.toString()

                            addInstructions(
                                index + 1,
                                smaliInstruction.replace(REGISTER_TEMPLATE_REPLACEMENT, register)
                            )
                        }
                }
            }
        }
    }
}

/**
 * Find the index of the first wide literal instruction with the given value.
 *
 * @return the first literal instruction with the value, or -1 if not found.
 */
fun Method.getWideLiteralInstructionIndex(literal: Long) = implementation?.let {
    it.instructions.indexOfFirst { instruction ->
        (instruction as? WideLiteralInstruction)?.wideLiteral == literal
    }
} ?: -1

fun Method.getEmptyStringInstructionIndex()
= getStringInstructionIndex("")

fun Method.getStringInstructionIndex(value: String) = implementation?.let {
    it.instructions.indexOfFirst { instruction ->
        instruction.opcode == Opcode.CONST_STRING
                && (instruction as? BuilderInstruction21c)?.reference.toString() == value
    }
} ?: -1

/**
 * Check if the method contains a literal with the given value.
 *
 * @return if the method contains a literal with the given value.
 */
fun Method.containsWideLiteralInstructionIndex(literal: Long) =
    getWideLiteralInstructionIndex(literal) >= 0

fun Method.containsMethodReferenceNameInstructionIndex(methodName: String) =
    getTargetIndexWithMethodReferenceName(methodName) >= 0

fun Method.containsReferenceInstructionIndex(reference: String) =
    getTargetIndexWithReference(reference) >= 0

/**
 * Traverse the class hierarchy starting from the given root class.
 *
 * @param targetClass the class to start traversing the class hierarchy from.
 * @param callback function that is called for every class in the hierarchy.
 */
fun BytecodeContext.traverseClassHierarchy(
    targetClass: MutableClass,
    callback: MutableClass.() -> Unit
) {
    callback(targetClass)
    this.findClass(targetClass.superclass ?: return)?.mutableClass?.let {
        traverseClassHierarchy(it, callback)
    }
}

/**
 * Get the [Reference] of an [Instruction] as [T].
 *
 * @param T The type of [Reference] to cast to.
 * @return The [Reference] as [T] or null
 * if the [Instruction] is not a [ReferenceInstruction] or the [Reference] is not of type [T].
 * @see ReferenceInstruction
 */
inline fun <reified T : Reference> Instruction.getReference() =
    (this as? ReferenceInstruction)?.reference as? T

/**
 * Get the index of the first [Instruction] that matches the predicate.
 *
 * @param predicate The predicate to match.
 * @return The index of the first [Instruction] that matches the predicate.
 */
fun Method.indexOfFirstInstruction(predicate: Instruction.() -> Boolean) =
    this.implementation!!.instructions.indexOfFirst(predicate)

fun MutableMethod.getTargetIndex(opcode: Opcode) = getTargetIndex(0, opcode)

fun MutableMethod.getTargetIndexReversed(opcode: Opcode) =
    getTargetIndexReversed(implementation!!.instructions.size - 1, opcode)

fun MutableMethod.getTargetIndex(startIndex: Int, opcode: Opcode) =
    implementation!!.instructions.let {
        startIndex + it.subList(startIndex, it.size - 1).indexOfFirst { instruction ->
            instruction.opcode == opcode
        }
    }

fun MutableMethod.getTargetIndexReversed(startIndex: Int, opcode: Opcode): Int {
    for (index in startIndex downTo 0) {
        if (getInstruction(index).opcode != opcode)
            continue

        return index
    }
    return -1
}

fun Method.getTargetIndexWithFieldReferenceName(filedName: String) = implementation?.let {
    it.instructions.indexOfFirst { instruction ->
        instruction.getReference<FieldReference>()?.name == filedName
    }
} ?: -1

fun MutableMethod.getTargetIndexWithFieldReferenceNameReversed(returnType: String)
        = getTargetIndexWithFieldReferenceTypeReversed(implementation!!.instructions.size - 1, returnType)

fun MutableMethod.getTargetIndexWithFieldReferenceName(startIndex: Int, filedName: String) =
    implementation!!.instructions.let {
        startIndex + it.subList(startIndex, it.size - 1).indexOfFirst { instruction ->
            instruction.getReference<FieldReference>()?.name == filedName
        }
    }

fun MutableMethod.getTargetIndexWithFieldReferenceNameReversed(startIndex: Int, filedName: String): Int {
    for (index in startIndex downTo 0) {
        val instruction = getInstruction(index)
        if (instruction.getReference<FieldReference>()?.name != filedName)
            continue

        return index
    }
    return -1
}

fun Method.getTargetIndexWithFieldReferenceType(returnType: String) = implementation?.let {
    it.instructions.indexOfFirst { instruction ->
        instruction.getReference<FieldReference>()?.type == returnType
    }
} ?: -1

fun MutableMethod.getTargetIndexWithFieldReferenceTypeReversed(returnType: String)
= getTargetIndexWithFieldReferenceTypeReversed(implementation!!.instructions.size - 1, returnType)

fun MutableMethod.getTargetIndexWithFieldReferenceType(startIndex: Int, returnType: String) =
    implementation!!.instructions.let {
        startIndex + it.subList(startIndex, it.size - 1).indexOfFirst { instruction ->
            instruction.getReference<FieldReference>()?.type == returnType
        }
    }

fun MutableMethod.getTargetIndexWithFieldReferenceTypeReversed(startIndex: Int, returnType: String): Int {
    for (index in startIndex downTo 0) {
        val instruction = getInstruction(index)
        if (instruction.getReference<FieldReference>()?.type != returnType)
            continue

        return index
    }
    return -1
}

fun Method.getTargetIndexWithMethodReferenceName(methodName: String) = implementation?.let {
    it.instructions.indexOfFirst { instruction ->
        instruction.getReference<MethodReference>()?.name == methodName
    }
} ?: -1

fun MutableMethod.getTargetIndexWithMethodReferenceNameReversed(methodName: String)
= getTargetIndexWithMethodReferenceNameReversed(implementation!!.instructions.size - 1, methodName)


fun MutableMethod.getTargetIndexWithMethodReferenceName(startIndex: Int, methodName: String) =
    implementation!!.instructions.let {
        startIndex + it.subList(startIndex, it.size - 1).indexOfFirst { instruction ->
            instruction.getReference<MethodReference>()?.name == methodName
        }
    }

fun MutableMethod.getTargetIndexWithMethodReferenceNameReversed(startIndex: Int, methodName: String): Int {
    for (index in startIndex downTo 0) {
        val instruction = getInstruction(index)
        if (instruction.getReference<MethodReference>()?.name != methodName)
            continue

        return index
    }
    return -1
}

fun Method.getTargetIndexWithReference(reference: String) = implementation?.let {
    it.instructions.indexOfFirst { instruction ->
        (instruction as? ReferenceInstruction)?.reference.toString().contains(reference)
    }
} ?: -1

fun MutableMethod.getTargetIndexWithReference(reference: String) =
    getTargetIndexWithReference(0, reference)

fun MutableMethod.getTargetIndexWithReferenceReversed(reference: String) =
    getTargetIndexWithReferenceReversed(implementation!!.instructions.size - 1, reference)

fun MutableMethod.getTargetIndexWithReference(startIndex: Int, reference: String) =
    implementation!!.instructions.let {
        startIndex + it.subList(startIndex, it.size - 1).indexOfFirst { instruction ->
            (instruction as? ReferenceInstruction)?.reference.toString().contains(reference)
        }
    }

fun MutableMethod.getTargetIndexWithReferenceReversed(startIndex: Int, reference: String): Int {
    for (index in startIndex downTo 0) {
        val instruction = getInstruction(index)
        if (!(instruction as? ReferenceInstruction)?.reference.toString().contains(reference))
            continue

        return index
    }
    return -1
}

fun MethodFingerprintResult.getWalkerMethod(context: BytecodeContext, index: Int) =
    mutableMethod.getWalkerMethod(context, index)

fun MutableMethod.getWalkerMethod(context: BytecodeContext, index: Int) =
    context.toMethodWalker(this)
        .nextMethod(index, true)
        .getMethod() as MutableMethod

fun MutableClass.addFieldAndInstructions(
    context: BytecodeContext,
    methodName: String,
    fieldName: String,
    objectClass: String,
    smaliInstructions: String,
    shouldAddConstructor: Boolean
) {
    val objectCall = "$this->$fieldName:$objectClass"

    methods.single { method -> method.name == methodName }.apply {
        staticFields.add(
            ImmutableField(
                definingClass,
                fieldName,
                objectClass,
                AccessFlags.PUBLIC or AccessFlags.STATIC,
                null,
                annotations,
                null
            ).toMutable()
        )

        addInstructionsWithLabels(
            0,
            """
                sget-object v0, $objectCall
                """ + smaliInstructions
        )
    }

    if (shouldAddConstructor) {
        context.findClass(objectClass)!!.mutableClass.methods
            .filter { method -> method.name == "<init>" }
            .forEach { mutableMethod ->
                mutableMethod.apply {
                    val initializeIndex = getTargetIndexWithMethodReferenceName("<init>")
                    val insertIndex = if (initializeIndex == -1)
                        1
                    else
                        initializeIndex + 1

                    addInstruction(
                        insertIndex,
                        "sput-object p0, $objectCall"
                    )
                }
            }
    }
}

fun BytecodeContext.updatePatchStatus(
    className: String,
    methodName: String
) {
    this.classes.forEach { classDef ->
        if (classDef.type.endsWith(className)) {
            val patchStatusMethod =
                this.proxy(classDef).mutableClass.methods.first { it.name == methodName }

            patchStatusMethod.replaceInstruction(
                0,
                "const/4 v0, 0x1"
            )
        }
    }
}

/**
 * Return the resolved methods of [MethodFingerprint]s early.
 */
fun List<MethodFingerprint>.returnEarly(bool: Boolean = false) {
    val const = if (bool) "0x1" else "0x0"
    this.forEach { fingerprint ->
        fingerprint.resultOrThrow().let { result ->
            val stringInstructions = when (result.method.returnType.first()) {
                'L' -> """
                        const/4 v0, $const
                        return-object v0
                        """
                'V' -> "return-void"
                'I', 'Z' -> """
                        const/4 v0, $const
                        return v0
                        """
                else -> throw Exception("This case should never happen.")
            }

            result.mutableMethod.addInstructions(0, stringInstructions)
        }
    }
}
