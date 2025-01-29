@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package app.revanced.util

import app.revanced.patcher.FingerprintBuilder
import app.revanced.patcher.Match
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.instructions
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.shared.mapping.get
import app.revanced.patches.shared.mapping.resourceMappingPatch
import app.revanced.patches.shared.mapping.resourceMappings
import app.revanced.util.Utils.printWarn
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodParameter
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
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.util.MethodUtil

const val REGISTER_TEMPLATE_REPLACEMENT: String = "REGISTER_INDEX"

fun parametersEqual(
    parameters1: Iterable<CharSequence>,
    parameters2: Iterable<CharSequence>
): Boolean {
    if (parameters1.count() != parameters2.count()) return false
    val iterator1 = parameters1.iterator()
    parameters2.forEach {
        if (!it.startsWith(iterator1.next())) return false
    }
    return true
}

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
    targetMethod: String,
) = addInstruction(
    insertIndex,
    "invoke-static { v$viewRegister }, $classDescriptor->$targetMethod(Landroid/view/View;)V",
)

/**
 * Inserts instructions at a given index, using the existing control flow label at that index.
 * Inserted instructions can have it's own control flow labels as well.
 *
 * Effectively this changes the code from:
 * :label
 * (original code)
 *
 * Into:
 * :label
 * (patch code)
 * (original code)
 */
internal fun MutableMethod.addInstructionsAtControlFlowLabel(
    insertIndex: Int,
    instructions: String,
) {
    // Duplicate original instruction and add to +1 index.
    addInstruction(insertIndex + 1, getInstruction(insertIndex))

    // Add patch code at same index as duplicated instruction,
    // so it uses the original instruction control flow label.
    addInstructionsWithLabels(insertIndex + 1, instructions)

    // Remove original non duplicated instruction.
    removeInstruction(insertIndex)

    // Original instruction is now after the inserted patch instructions,
    // and the original control flow label is on the first instruction of the patch code.
}

/**
 * Get the index of the first instruction with the id of the given resource id name.
 *
 * Requires [resourceMappingPatch] as a dependency.
 *
 * @param resourceName the name of the resource to find the id for.
 * @return the index of the first instruction with the id of the given resource name, or -1 if not found.
 * @throws PatchException if the resource cannot be found.
 * @see [indexOfFirstResourceIdOrThrow], [indexOfFirstLiteralInstructionReversed]
 */
fun Method.indexOfFirstResourceId(resourceName: String): Int {
    val resourceId = resourceMappings["id", resourceName]
    if (resourceId == -1L) {
        printWarn("Could not find resource type: id name: $name")
        return -1
    }
    return indexOfFirstLiteralInstruction(resourceId)
}

/**
 * Get the index of the first instruction with the id of the given resource name or throw a [PatchException].
 *
 * Requires [resourceMappingPatch] as a dependency.
 *
 * @throws [PatchException] if the resource is not found, or the method does not contain the resource id literal value.
 * @see [indexOfFirstResourceId], [indexOfFirstLiteralInstructionReversedOrThrow]
 */
fun Method.indexOfFirstResourceIdOrThrow(resourceName: String): Int {
    val index = indexOfFirstResourceId(resourceName)
    if (index < 0) {
        throw PatchException("Found resource id for: '$resourceName' but method does not contain the id: $this")
    }

    return index
}

/**
 * Find the index of the first literal instruction with the given value.
 *
 * @return the first literal instruction with the value, or -1 if not found.
 * @see indexOfFirstLiteralInstructionOrThrow
 */
fun Method.indexOfFirstLiteralInstruction(literal: Long) = implementation?.let {
    it.instructions.indexOfFirst { instruction ->
        (instruction as? WideLiteralInstruction)?.wideLiteral == literal
    }
} ?: -1

/**
 * Find the index of the first literal instruction with the given value,
 * or throw an exception if not found.
 *
 * @return the first literal instruction with the value, or throws [PatchException] if not found.
 */
fun Method.indexOfFirstLiteralInstructionOrThrow(literal: Long): Int {
    val index = indexOfFirstLiteralInstruction(literal)
    if (index < 0) throw PatchException("Could not find literal value: $literal")
    return index
}

/**
 * Find the index of the last literal instruction with the given value.
 *
 * @return the last literal instruction with the value, or -1 if not found.
 * @see indexOfFirstLiteralInstructionOrThrow
 */
fun Method.indexOfFirstLiteralInstructionReversed(literal: Long) = implementation?.let {
    it.instructions.indexOfLast { instruction ->
        (instruction as? WideLiteralInstruction)?.wideLiteral == literal
    }
} ?: -1

/**
 * Find the index of the last wide literal instruction with the given value,
 * or throw an exception if not found.
 *
 * @return the last literal instruction with the value, or throws [PatchException] if not found.
 */
fun Method.indexOfFirstLiteralInstructionReversedOrThrow(literal: Long): Int {
    val index = indexOfFirstLiteralInstructionReversed(literal)
    if (index < 0) throw PatchException("Could not find literal value: $literal")
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
fun Method.containsLiteralInstruction(literal: Long) =
    indexOfFirstLiteralInstruction(literal) >= 0

/**
 * Traverse the class hierarchy starting from the given root class.
 *
 * @param targetClass the class to start traversing the class hierarchy from.
 * @param callback function that is called for every class in the hierarchy.
 */
fun BytecodePatchContext.traverseClassHierarchy(
    targetClass: MutableClass,
    callback: MutableClass.() -> Unit
) {
    callback(targetClass)

    targetClass.superclass ?: return

    classBy { targetClass.superclass == it.type }?.mutableClass?.let {
        traverseClassHierarchy(it, callback)
    }
}

fun MutableMethod.injectLiteralInstructionViewCall(
    literal: Long,
    smaliInstruction: String
) {
    val literalIndex = indexOfFirstLiteralInstructionOrThrow(literal)
    val targetIndex = indexOfFirstInstructionOrThrow(literalIndex, Opcode.MOVE_RESULT_OBJECT)
    val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA.toString()

    addInstructions(
        targetIndex + 1,
        smaliInstruction.replace(REGISTER_TEMPLATE_REPLACEMENT, targetRegister)
    )
}

fun BytecodePatchContext.replaceLiteralInstructionCall(
    originalLiteral: Long,
    replaceLiteral: Long
) {
    classes.forEach { classDef ->
        classDef.methods.forEach { method ->
            method.implementation.apply {
                this?.instructions?.forEachIndexed { _, instruction ->
                    if (instruction.opcode != Opcode.CONST)
                        return@forEachIndexed
                    if ((instruction as Instruction31i).wideLiteral != originalLiteral)
                        return@forEachIndexed

                    proxy(classDef)
                        .mutableClass
                        .findMutableMethodOf(method).apply {
                            val index = indexOfFirstLiteralInstructionOrThrow(originalLiteral)
                            val register =
                                (instruction as OneRegisterInstruction).registerA

                            replaceInstruction(index, "const v$register, $replaceLiteral")
                        }
                }
            }
        }
    }
}

fun BytecodePatchContext.replaceLiteralInstructionCall(
    literal: Long,
    smaliInstruction: String
) {
    classes.forEach { classDef ->
        classDef.methods.forEach { method ->
            method.implementation.apply {
                this?.instructions?.forEachIndexed { _, instruction ->
                    if (instruction.opcode != Opcode.CONST)
                        return@forEachIndexed
                    if ((instruction as Instruction31i).wideLiteral != literal)
                        return@forEachIndexed

                    proxy(classDef)
                        .mutableClass
                        .findMutableMethodOf(method).apply {
                            val index = indexOfFirstLiteralInstructionOrThrow(literal)
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
 * @return The index of the first opcode specified, or -1 if not found.
 * @see indexOfFirstInstructionOrThrow
 */
fun Method.indexOfFirstInstruction(targetOpcode: Opcode): Int =
    indexOfFirstInstruction(0, targetOpcode)

/**
 * @param startIndex Optional starting index to start searching from.
 * @return The index of the first opcode specified, or -1 if not found.
 * @see indexOfFirstInstructionOrThrow
 */
fun Method.indexOfFirstInstruction(startIndex: Int = 0, targetOpcode: Opcode): Int =
    indexOfFirstInstruction(startIndex) {
        opcode == targetOpcode
    }

/**
 * Get the index of the first [Instruction] that matches the predicate, starting from [startIndex].
 *
 * @param startIndex Optional starting index to start searching from.
 * @return -1 if the instruction is not found.
 * @see indexOfFirstInstructionOrThrow
 */
fun Method.indexOfFirstInstruction(startIndex: Int = 0, filter: Instruction.() -> Boolean): Int {
    if (implementation == null) {
        return -1
    }
    var instructions = this.implementation!!.instructions
    if (startIndex != 0) {
        instructions = instructions.drop(startIndex)
    }
    val index = instructions.indexOfFirst(filter)

    return if (index >= 0) {
        startIndex + index
    } else {
        -1
    }
}

/**
 * @return The index of the first opcode specified
 * @throws PatchException
 * @see indexOfFirstInstruction
 */
fun Method.indexOfFirstInstructionOrThrow(targetOpcode: Opcode): Int =
    indexOfFirstInstructionOrThrow(0, targetOpcode)

/**
 * @return The index of the first opcode specified, starting from the index specified.
 * @throws PatchException
 * @see indexOfFirstInstruction
 */
fun Method.indexOfFirstInstructionOrThrow(startIndex: Int = 0, targetOpcode: Opcode): Int =
    indexOfFirstInstructionOrThrow(startIndex) {
        opcode == targetOpcode
    }

/**
 * Get the index of the first [Instruction] that matches the predicate, starting from [startIndex].
 *
 * @return the index of the instruction
 * @throws PatchException
 * @see indexOfFirstInstruction
 */
fun Method.indexOfFirstInstructionOrThrow(
    startIndex: Int = 0,
    filter: Instruction.() -> Boolean
): Int {
    val index = indexOfFirstInstruction(startIndex, filter)
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
fun Method.indexOfFirstInstructionReversed(startIndex: Int? = null, targetOpcode: Opcode): Int =
    indexOfFirstInstructionReversed(startIndex) {
        opcode == targetOpcode
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
    filter: Instruction.() -> Boolean
): Int {
    if (implementation == null) {
        return -1
    }
    var instructions = this.implementation!!.instructions
    if (startIndex != null) {
        instructions = instructions.take(startIndex + 1)
    }

    return instructions.indexOfLast(filter)
}

fun Method.indexOfFirstInstructionReversedOrThrow(opcode: Opcode): Int =
    indexOfFirstInstructionReversedOrThrow(null, opcode)

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
    targetOpcode: Opcode
): Int =
    indexOfFirstInstructionReversedOrThrow(startIndex) {
        opcode == targetOpcode
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
    filter: Instruction.() -> Boolean
): Int {
    val index = indexOfFirstInstructionReversed(startIndex, filter)

    if (index < 0) {
        throw PatchException("Could not find instruction index")
    }

    return index
}

/**
 * @return An immutable list of indices of the instructions in reverse order.
 *  _Returns an empty list if no indices are found_
 *  @see findInstructionIndicesReversedOrThrow
 */
fun Method.findInstructionIndicesReversed(filter: Instruction.() -> Boolean): List<Int> =
    instructions
        .withIndex()
        .filter { (_, instruction) -> filter(instruction) }
        .map { (index, _) -> index }
        .asReversed()

/**
 * @return An immutable list of indices of the instructions in reverse order.
 * @throws PatchException if no matching indices are found.
 */
fun Method.findInstructionIndicesReversedOrThrow(filter: Instruction.() -> Boolean): List<Int> {
    val indexes = findInstructionIndicesReversed(filter)
    if (indexes.isEmpty()) throw PatchException("No matching instructions found in: $this")

    return indexes
}

/**
 * @return An immutable list of indices of the opcode in reverse order.
 *  _Returns an empty list if no indices are found_
 * @see findInstructionIndicesReversedOrThrow
 */
fun Method.findInstructionIndicesReversed(opcode: Opcode): List<Int> =
    findInstructionIndicesReversed { this.opcode == opcode }

/**
 * @return An immutable list of indices of the opcode in reverse order.
 * @throws PatchException if no matching indices are found.
 */
fun Method.findInstructionIndicesReversedOrThrow(opcode: Opcode): List<Int> {
    val instructions = findInstructionIndicesReversed(opcode)
    if (instructions.isEmpty()) throw PatchException("Could not find opcode: $opcode in: $this")

    return instructions
}

fun Method.referenceMatchesOrThrow(targetIndex: Int, reference: String) {
    val targetReference = getInstruction<ReferenceInstruction>(targetIndex).reference.toString()
    if (reference != targetReference) throw PatchException("References do not match. Expected: '$reference', Found: '$targetReference'")
}

/**
 * Called for _all_ instructions with the given literal value.
 */
fun BytecodePatchContext.forEachLiteralValueInstruction(
    literal: Long,
    block: MutableMethod.(literalInstructionIndex: Int) -> Unit,
) {
    classes.forEach { classDef ->
        classDef.methods.forEach { method ->
            method.implementation?.instructions?.forEachIndexed { index, instruction ->
                if (instruction.opcode == Opcode.CONST &&
                    (instruction as WideLiteralInstruction).wideLiteral == literal
                ) {
                    val mutableMethod = proxy(classDef).mutableClass.findMutableMethodOf(method)
                    block.invoke(mutableMethod, index)
                }
            }
        }
    }
}

context(BytecodePatchContext)
fun Match.getWalkerMethod(offset: Int) =
    method.getWalkerMethod(offset)

context(BytecodePatchContext)
fun MutableMethod.getWalkerMethod(offset: Int): MutableMethod {
    val newMethod = getInstruction<ReferenceInstruction>(offset).reference as MethodReference
    return findMethodOrThrow(newMethod.definingClass) {
        MethodUtil.methodSignaturesMatch(this, newMethod)
    }
}

/**
 * Taken from BiliRoamingX:
 * https://github.com/BiliRoamingX/BiliRoamingX/blob/ae58109f3acdd53ec2d2b3fb439c2a2ef1886221/patches/src/main/kotlin/app/revanced/patches/bilibili/utils/Extenstions.kt#L151
 */
fun MutableMethod.getFiveRegisters(index: Int) =
    with(getInstruction<FiveRegisterInstruction>(index)) {
        arrayOf(registerC, registerD, registerE, registerF, registerG)
            .take(registerCount).joinToString(",") { "v$it" }
    }

context(BytecodePatchContext)
fun addStaticFieldToExtension(
    className: String,
    methodName: String,
    fieldName: String,
    objectClass: String,
    smaliInstructions: String,
    shouldAddConstructor: Boolean = true
) {
    val classDef = classes.find { classDef -> classDef.type == className }
        ?: throw PatchException("No matching methods found in: $className")
    val mutableClass = proxy(classDef).mutableClass

    val objectCall = "$mutableClass->$fieldName:$objectClass"

    mutableClass.apply {
        methods.first { method -> method.name == methodName }.apply {
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
    }

    if (!shouldAddConstructor) return

    findMethodsOrThrow(objectClass)
        .filter { method -> MethodUtil.isConstructor(method) }
        .forEach { mutableMethod ->
            mutableMethod.apply {
                val initializeIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_DIRECT &&
                            getReference<MethodReference>()?.name == "<init>"
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

context(BytecodePatchContext)
fun findMethodOrThrow(
    reference: String,
    methodPredicate: Method.() -> Boolean = { MethodUtil.isConstructor(this) }
) = findMethodsOrThrow(reference).first(methodPredicate)

context(BytecodePatchContext)
fun findMethodsOrThrow(reference: String): MutableSet<MutableMethod> {
    val classDef = classes.find { classDef -> classDef.type == reference }
        ?: throw PatchException("No matching methods found in: $reference")
    return proxy(classDef)
        .mutableClass
        .methods
}

context(BytecodePatchContext)
fun updatePatchStatus(
    className: String,
    methodName: String
) = findMethodOrThrow(className) { name == methodName }
    .replaceInstruction(
        0,
        "const/4 v0, 0x1"
    )

/**
 * Taken from BiliRoamingX:
 * https://github.com/BiliRoamingX/BiliRoamingX/blob/ae58109f3acdd53ec2d2b3fb439c2a2ef1886221/patches/src/main/kotlin/app/revanced/patches/bilibili/utils/Extenstions.kt#L51
 */
fun Method.cloneMutable(
    registerCount: Int = implementation?.registerCount ?: 0,
    clearImplementation: Boolean = false,
    name: String = this.name,
    accessFlags: Int = this.accessFlags,
    parameters: List<MethodParameter> = this.parameters,
    returnType: String = this.returnType
): MutableMethod {
    val clonedImplementation = implementation?.let {
        ImmutableMethodImplementation(
            registerCount,
            if (clearImplementation) emptyList() else it.instructions,
            if (clearImplementation) emptyList() else it.tryBlocks,
            if (clearImplementation) emptyList() else it.debugItems,
        )
    }
    return ImmutableMethod(
        definingClass,
        name,
        parameters,
        returnType,
        accessFlags,
        annotations,
        hiddenApiRestrictions,
        clonedImplementation
    ).toMutable()
}

/**
 * Return the method early.
 */
fun MutableMethod.returnEarly(bool: Boolean = false) {
    val const = if (bool) "0x1" else "0x0"

    val stringInstructions = when (returnType.first()) {
        'L' ->
            """
                const/4 v0, $const
                return-object v0
            """

        'V' -> "return-void"
        'I', 'Z' ->
            """
                const/4 v0, $const
                return v0
            """

        else -> throw Exception("This case should never happen.")
    }

    addInstructions(0, stringInstructions)
}

/**
 * Set the custom condition for this fingerprint to check for a literal value.
 *
 * @param literalSupplier The supplier for the literal value to check for.
 */
// TODO: add a way for subclasses to also use their own custom fingerprint.
fun FingerprintBuilder.literal(literalSupplier: () -> Long) {
    custom { method, _ ->
        method.containsLiteralInstruction(literalSupplier())
    }
}

/**
 * Perform a bitwise OR operation between an [AccessFlags] and an [Int].
 *
 * @param other The [Int] to perform the operation with.
 */
infix fun Int.or(other: AccessFlags) = this or other.value

/**
 * Perform a bitwise OR operation between two [AccessFlags].
 *
 * @param other The other [AccessFlags] to perform the operation with.
 */
infix fun AccessFlags.or(other: AccessFlags) = value or other.value

/**
 * Perform a bitwise OR operation between an [Int] and an [AccessFlags].
 *
 * @param other The [AccessFlags] to perform the operation with.
 */
infix fun AccessFlags.or(other: Int) = value or other