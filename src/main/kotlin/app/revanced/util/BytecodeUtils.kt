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
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31i
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.util.MethodUtil

const val REGISTER_TEMPLATE_REPLACEMENT: String = "REGISTER_INDEX"

fun MethodFingerprint.isDeprecated() =
    javaClass.annotations[0].toString().contains("Deprecated")

fun MethodFingerprint.resultOrThrow() = result ?: throw exception

/**
 * The [PatchException] of failing to resolve a [MethodFingerprint].
 *
 * @return The [PatchException].
 */
val MethodFingerprint.exception
    get() = PatchException("Failed to resolve ${this.javaClass.simpleName}")

fun MethodFingerprint.alsoResolve(context: BytecodeContext, fingerprint: MethodFingerprint) =
    also { resolve(context, fingerprint.resultOrThrow().classDef) }.resultOrThrow()

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

fun MethodFingerprint.injectLiteralInstructionBooleanCall(
    literal: Int,
    descriptor: String
) = injectLiteralInstructionBooleanCall(literal.toLong(), descriptor)

fun MethodFingerprint.injectLiteralInstructionBooleanCall(
    literal: Long,
    descriptor: String
) {
    resultOrThrow().mutableMethod.apply {
        val literalIndex = indexOfFirstWideLiteralInstructionValueOrThrow(literal)
        val targetIndex = indexOfFirstInstructionOrThrow(literalIndex, Opcode.MOVE_RESULT)
        val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

        val smaliInstruction =
            if (descriptor.startsWith("0x")) """
                const/16 v$targetRegister, $descriptor
                """
            else if (descriptor.endsWith("(Z)Z")) """
                invoke-static {v$targetRegister}, $descriptor
                move-result v$targetRegister
                """
            else """
                invoke-static {}, $descriptor
                move-result v$targetRegister
                """

        addInstructions(
            targetIndex + 1,
            smaliInstruction
        )
    }
}

fun MethodFingerprint.injectLiteralInstructionViewCall(
    literal: Long,
    smaliInstruction: String
) = resultOrThrow().mutableMethod.injectLiteralInstructionViewCall(literal, smaliInstruction)

fun MutableMethod.injectLiteralInstructionViewCall(
    literal: Long,
    smaliInstruction: String
) {
    val literalIndex = indexOfFirstWideLiteralInstructionValueOrThrow(literal)
    val targetIndex = indexOfFirstInstructionOrThrow(literalIndex, Opcode.MOVE_RESULT_OBJECT)
    val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA.toString()

    addInstructions(
        targetIndex + 1,
        smaliInstruction.replace(REGISTER_TEMPLATE_REPLACEMENT, targetRegister)
    )
}

fun BytecodeContext.injectLiteralInstructionViewCall(
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
                        .injectLiteralInstructionViewCall(literal, smaliInstruction)
                }
            }
        }
    }
}

fun BytecodeContext.replaceLiteralInstructionCall(
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
                            val index = indexOfFirstWideLiteralInstructionValueOrThrow(literal)
                            val register =
                                (instruction as OneRegisterInstruction).registerA.toString()

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
 * Get the index of the first [Instruction] that matches the predicate, starting from [startIndex].
 *
 * @param startIndex Optional starting index to start searching from.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionOrThrow
 */
fun Method.indexOfFirstInstruction(startIndex: Int = 0, opcode: Opcode): Int =
    indexOfFirstInstruction(startIndex) {
        this.opcode == opcode
    }

/**
 * Get the index of the first [Instruction] that matches the predicate, starting from [startIndex].
 *
 * @param startIndex Optional starting index to start searching from.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionOrThrow
 */
fun Method.indexOfFirstInstruction(startIndex: Int = 0, predicate: Instruction.() -> Boolean): Int {
    if (implementation == null) {
        return -1
    }
    var instructions = implementation!!.instructions
    if (startIndex != 0) {
        instructions = instructions.drop(startIndex)
    }
    val index = instructions.indexOfFirst(predicate)

    return if (index >= 0) {
        startIndex + index
    } else {
        -1
    }
}

fun Method.indexOfFirstInstructionOrThrow(opcode: Opcode): Int =
    indexOfFirstInstructionOrThrow(0, opcode)

/**
 * Get the index of the first [Instruction] that matches the predicate, starting from [startIndex].
 *
 * @return the index of the instruction
 * @throws PatchException
 * @see indexOfFirstInstruction
 */
fun Method.indexOfFirstInstructionOrThrow(startIndex: Int = 0, opcode: Opcode): Int =
    indexOfFirstInstructionOrThrow(startIndex) {
        this.opcode == opcode
    }

fun Method.indexOfFirstInstructionReversedOrThrow(opcode: Opcode): Int =
    indexOfFirstInstructionReversedOrThrow(null, opcode)

/**
 * Get the index of the first [Instruction] that matches the predicate, starting from [startIndex].
 *
 * @return the index of the instruction
 * @throws PatchException
 * @see indexOfFirstInstruction
 */
fun Method.indexOfFirstInstructionOrThrow(
    startIndex: Int = 0,
    predicate: Instruction.() -> Boolean
): Int {
    val index = indexOfFirstInstruction(startIndex, predicate)
    if (index < 0) {
        throw PatchException("Could not find instruction index")
    }
    return index
}

/**
 * Get the index of matching instruction,
 * starting from and [startIndex] and searching down.
 *
 * @param startIndex Optional starting index to search down from. Searching includes the start index.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionReversedOrThrow
 */
fun Method.indexOfFirstInstructionReversed(startIndex: Int? = null, opcode: Opcode): Int =
    indexOfFirstInstructionReversed(startIndex) {
        this.opcode == opcode
    }

/**
 * Get the index of matching instruction,
 * starting from and [startIndex] and searching down.
 *
 * @param startIndex Optional starting index to search down from. Searching includes the start index.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionReversedOrThrow
 */
fun Method.indexOfFirstInstructionReversed(
    startIndex: Int? = null,
    predicate: Instruction.() -> Boolean
): Int {
    if (implementation == null) {
        return -1
    }
    var instructions = implementation!!.instructions
    if (startIndex != null) {
        instructions = instructions.take(startIndex + 1)
    }

    return instructions.indexOfLast(predicate)
}

/**
 * Get the index of matching instruction,
 * starting from and [startIndex] and searching down.
 *
 * @param startIndex Optional starting index to search down from. Searching includes the start index.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionReversed
 */
fun Method.indexOfFirstInstructionReversedOrThrow(
    startIndex: Int? = null,
    opcode: Opcode
): Int =
    indexOfFirstInstructionReversedOrThrow(startIndex) {
        this.opcode == opcode
    }

/**
 * Get the index of matching instruction,
 * starting from and [startIndex] and searching down.
 *
 * @param startIndex Optional starting index to search down from. Searching includes the start index.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionReversed
 */
fun Method.indexOfFirstInstructionReversedOrThrow(
    startIndex: Int? = null,
    predicate: Instruction.() -> Boolean
): Int {
    val index = indexOfFirstInstructionReversed(startIndex, predicate)

    if (index < 0) {
        throw PatchException("Could not find instruction index")
    }

    return index
}

/**
 * @return The list of indices of the opcode in reverse order.
 */
fun Method.findOpcodeIndicesReversed(opcode: Opcode): List<Int> =
    findOpcodeIndicesReversed { this.opcode == opcode }

/**
 * @return The list of indices of the opcode in reverse order.
 */
fun Method.findOpcodeIndicesReversed(filter: Instruction.() -> Boolean): List<Int> {
    val indexes = implementation!!.instructions
        .withIndex()
        .filter { (_, instruction) -> filter.invoke(instruction) }
        .map { (index, _) -> index }
        .reversed()

    if (indexes.isEmpty()) throw PatchException("No matching instructions found in: $this")

    return indexes
}

/**
 * Find the index of the first wide literal instruction with the given value.
 *
 * @return the first literal instruction with the value, or -1 if not found.
 * @see indexOfFirstWideLiteralInstructionValueOrThrow
 */
fun Method.indexOfFirstWideLiteralInstructionValue(literal: Long) = implementation?.let {
    it.instructions.indexOfFirst { instruction ->
        (instruction as? WideLiteralInstruction)?.wideLiteral == literal
    }
} ?: -1


/**
 * Find the index of the first wide literal instruction with the given value,
 * or throw an exception if not found.
 *
 * @return the first literal instruction with the value, or throws [PatchException] if not found.
 */
fun Method.indexOfFirstWideLiteralInstructionValueOrThrow(literal: Long): Int {
    val index = indexOfFirstWideLiteralInstructionValue(literal)
    if (index < 0) {
        val value =
            if (literal >= 2130706432) // 0x7f000000, general resource id
                String.format("%#X", literal).lowercase()
            else
                literal.toString()

        throw PatchException("Found literal value: '$value' but method does not contain the id: $this")
    }

    return index
}

fun Method.indexOfFirstStringInstruction(str: String) =
    indexOfFirstInstruction {
        opcode == Opcode.CONST_STRING &&
                getReference<StringReference>()?.string == str
    }


fun Method.indexOfFirstStringInstructionOrThrow(str: String): Int {
    val index = indexOfFirstStringInstruction(str)
    if (index < 0) {
        throw PatchException("Found string value for: '$str' but method does not contain the id: $this")
    }

    return index
}

/**
 * Check if the method contains a literal with the given value.
 *
 * @return if the method contains a literal with the given value.
 */
fun Method.containsWideLiteralInstructionValue(literal: Long) =
    indexOfFirstWideLiteralInstructionValue(literal) >= 0

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

fun MethodFingerprintResult.getWalkerMethod(context: BytecodeContext, offset: Int) =
    mutableMethod.getWalkerMethod(context, offset)

/**
 * MethodWalker can find the wrong class:
 * https://github.com/ReVanced/revanced-patcher/issues/309
 *
 * As a workaround, redefine MethodWalker here
 */
fun MutableMethod.getWalkerMethod(context: BytecodeContext, offset: Int): MutableMethod {
    val newMethod = getInstruction<ReferenceInstruction>(offset).reference as MethodReference
    return context.findClass { classDef -> classDef.type == newMethod.definingClass }
        ?.mutableClass
        ?.methods
        ?.first { method -> MethodUtil.methodSignaturesMatch(method, newMethod) }
        ?: throw PatchException("This method can not be walked at offset $offset inside the method $name")
}

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
                    val initializeIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.INVOKE_DIRECT && getReference<MethodReference>()?.name == "<init>"
                    }
                    val insertIndex = if (initializeIndex == -1)
                        1
                    else
                        initializeIndex + 1

                    val initializeRegister = if (initializeIndex == -1)
                        "p0"
                    else
                        "v${getInstruction<FiveRegisterInstruction>(initializeIndex).registerC}"

                    addInstruction(
                        insertIndex,
                        "sput-object $initializeRegister, $objectCall"
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
