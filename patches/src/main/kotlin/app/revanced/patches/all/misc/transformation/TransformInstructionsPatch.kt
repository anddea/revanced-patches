package app.revanced.patches.all.misc.transformation

import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.util.findMutableMethodOf
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction

private const val EXTENSION_NAME_SPACE_PATH =
    "Lapp/revanced/extension/"

fun <T> transformInstructionsPatch(
    filterMap: (ClassDef, Method, Instruction, Int) -> T?,
    transform: (MutableMethod, T) -> Unit,
    executeBlock: BytecodePatchContext.() -> Unit = {},
    // If the instructions of Extension are replaced, the patch may not work as intended.
    skipExtension: Boolean = true,
) = bytecodePatch(
    description = "transformInstructionsPatch"
) {
    // Returns the patch indices as a Sequence, which will execute lazily.
    fun findPatchIndices(classDef: ClassDef, method: Method): Sequence<T>? =
        method.implementation?.instructions?.asSequence()?.withIndex()
            ?.mapNotNull { (index, instruction) ->
                filterMap(classDef, method, instruction, index)
            }

    execute {
        // Find all methods to patch
        buildMap {
            classes.forEach { classDef ->
                if (skipExtension && classDef.type.startsWith(EXTENSION_NAME_SPACE_PATH)) {
                    return@forEach
                }
                val methods = buildList {
                    classDef.methods.forEach { method ->
                        // Since the Sequence executes lazily,
                        // using any() results in only calling
                        // filterMap until the first index has been found.
                        if (findPatchIndices(classDef, method)?.any() == true) add(method)
                    }
                }

                if (methods.isNotEmpty()) {
                    put(classDef, methods)
                }
            }
        }.forEach { (classDef, methods) ->
            // And finally transform the methods...
            val mutableClass = proxy(classDef).mutableClass

            methods.map(mutableClass::findMutableMethodOf).forEach methods@{ mutableMethod ->
                val patchIndices =
                    findPatchIndices(mutableClass, mutableMethod)?.toCollection(ArrayDeque())
                        ?: return@methods

                while (!patchIndices.isEmpty()) transform(mutableMethod, patchIndices.removeLast())
            }
        }

        executeBlock()
    }
}
