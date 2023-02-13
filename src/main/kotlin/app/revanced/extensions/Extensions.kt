package app.revanced.extensions

import app.revanced.patcher.extensions.MethodFingerprintExtensions.name
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableField
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.toInstruction
import app.revanced.util.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.util.integrations.Constants.PATCHES_PATH
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.util.MethodUtil
import org.w3c.dom.Node

internal fun MutableMethodImplementation.injectHideCall(
    index: Int,
    register: Int,
    patches: String,
    method: String
) {
    this.addInstruction(
        index,
        "invoke-static { v$register }, $PATCHES_PATH/$patches;->$method(Landroid/view/View;)V".toInstruction()
    )
}

internal fun MutableMethodImplementation.injectTheme(
    index: Int,
    register: Int,
    method: String
) {
    this.addInstruction(
        index,
        "invoke-static { v$register }, $INTEGRATIONS_PATH/utils/ThemeHelper;->$method(I)V".toInstruction()
    )
}

// TODO: populate this to all patches
/**
 * Convert a [MethodFingerprint] to a [PatchResultError].
 *
 * @return A [PatchResultError] for the [MethodFingerprint].
 */
fun MethodFingerprint.toErrorResult() = PatchResultError("Failed to resolve $name")

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
 * apply a transform to all methods of the class
 *
 * @param transform the transformation function. original method goes in, transformed method goes out
 */
fun MutableClass.transformMethods(transform: MutableMethod.() -> MutableMethod) {
    val transformedMethods = methods.map { it.transform() }
    methods.clear()
    methods.addAll(transformedMethods)
}

/**
 * apply a transform to all fields of the class
 *
 * @param transform the transformation function. original field goes in, transformed field goes out
 */
fun MutableClass.transformFields(transform: MutableField.() -> MutableField) {
    val transformedFields = fields.map { it.transform() }
    fields.clear()
    fields.addAll(transformedFields)
}

internal fun Node.doRecursively(action: (Node) -> Unit) {
    action(this)
    for (i in 0 until this.childNodes.length) this.childNodes.item(i).doRecursively(action)
}

internal fun String.startsWithAny(vararg prefixes: String): Boolean {
    for (prefix in prefixes)
        if (this.startsWith(prefix))
            return true

    return false
}

internal fun toResult(errorIndex: Int): PatchResult {
    if (errorIndex == -1)
        return PatchResultSuccess()
    else
        return PatchResultError("Instruction not found: $errorIndex")
}

