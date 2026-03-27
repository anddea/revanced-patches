package app.morphe.patches.shared.extension

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.FingerprintBuilder
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.fingerprint
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.encodedValue.MutableLongEncodedValue
import app.morphe.patches.shared.extension.Constants.EXTENSION_PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import app.morphe.util.findMethodsOrThrow
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.immutable.value.ImmutableLongEncodedValue
import java.util.jar.Manifest

fun sharedExtensionPatch(
    vararg hooks: ExtensionHook,
) = bytecodePatch(
    description = "sharedExtensionPatch"
) {
    extendWith("extensions/shared.mpe")

    execute {
        if (classDefByOrNull { EXTENSION_UTILS_CLASS_DESCRIPTOR == it.type } == null) {
            throw PatchException(
                "Shared extension has not been merged yet. This patch can not succeed without merging it.",
            )
        }
        hooks.forEach { hook -> hook(EXTENSION_UTILS_CLASS_DESCRIPTOR) }
    }

    finalize {
        findMethodsOrThrow(EXTENSION_PATCH_STATUS_CLASS_DESCRIPTOR).apply {
            find { method -> method.name == "PatchedTime" }
                ?.replaceInstruction(
                    0,
                    "const-wide v0, ${MutableLongEncodedValue(ImmutableLongEncodedValue(System.currentTimeMillis()))}L"
                )

            find { method -> method.name == "PatchVersion" }
                ?.apply {
                    val manifest = object {}
                        .javaClass
                        .classLoader
                        .getResources("META-INF/MANIFEST.MF")

                    while (manifest.hasMoreElements()) {
                        Manifest(manifest.nextElement().openStream())
                            .mainAttributes
                            .getValue("Version")
                            ?.let {
                                returnEarly(it)
                                return@finalize
                            }
                    }
                }
        }
    }
}

@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
class ExtensionHook internal constructor(
    val fingerprint: Fingerprint,
    private val insertIndexResolver: ((Method) -> Int),
    private val contextRegisterResolver: (Method) -> String,
) {
    context(BytecodePatchContext)
    operator fun invoke(extensionClassDescriptor: String) {
        val insertIndex = insertIndexResolver(fingerprint.method)
        val contextRegister = contextRegisterResolver(fingerprint.method)

        fingerprint.method.addInstruction(
            insertIndex,
            "invoke-static/range { $contextRegister .. $contextRegister }, " +
                    "$extensionClassDescriptor->setContext(Landroid/content/Context;)V",
        )
    }
}

fun extensionHook(
    insertIndexResolver: ((Method) -> Int) = { 0 },
    contextRegisterResolver: (Method) -> String = { "p0" },
    fingerprint: Fingerprint,
) = ExtensionHook(
    fingerprint,
    insertIndexResolver,
    contextRegisterResolver
)
