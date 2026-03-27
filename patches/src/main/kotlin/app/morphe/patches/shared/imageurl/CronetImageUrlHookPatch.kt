package app.morphe.patches.shared.imageurl

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.extension.Constants.PATCHES_PATH
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.fingerprint.mutableClassOrThrow
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val EXTENSION_SHARED_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/BypassImageRegionRestrictionsPatch;"

private lateinit var loadImageUrlMethod: MutableMethod
private var loadImageUrlIndex = 0

private lateinit var loadImageSuccessCallbackMethod: MutableMethod
private var loadImageSuccessCallbackIndex = 0

private lateinit var loadImageErrorCallbackMethod: MutableMethod
private var loadImageErrorCallbackIndex = 0

fun cronetImageUrlHookPatch(
    resolveCronetRequest: Boolean,
) = bytecodePatch(
    description = "cronetImageUrlHookPatch",
) {
    execute {
        loadImageUrlMethod = messageDigestImageUrlFingerprint
            .matchOrThrow(messageDigestImageUrlParentFingerprint).method

        if (!resolveCronetRequest) return@execute

        loadImageSuccessCallbackMethod = onSucceededFingerprint
            .matchOrThrow(onResponseStartedFingerprint).method

        loadImageErrorCallbackMethod = onFailureFingerprint
            .matchOrThrow(onResponseStartedFingerprint).method

        // The URL is required for the failure callback hook, but the URL field is obfuscated.
        // Add a helper get method that returns the URL field.
        requestFingerprint.methodOrThrow().apply {
            // The url is the only string field that is set inside the constructor.
            val urlFieldInstruction = instructions.first {
                val reference = it.getReference<FieldReference>()
                it.opcode == Opcode.IPUT_OBJECT && reference?.type == "Ljava/lang/String;"
            } as ReferenceInstruction

            val urlFieldName = (urlFieldInstruction.reference as FieldReference).name
            val definingClass = CRONET_URL_REQUEST_CLASS_DESCRIPTOR
            val addedMethodName = "getHookedUrl"
            requestFingerprint.mutableClassOrThrow().methods.add(
                ImmutableMethod(
                    definingClass,
                    addedMethodName,
                    emptyList(),
                    "Ljava/lang/String;",
                    AccessFlags.PUBLIC.value,
                    null,
                    null,
                    MutableMethodImplementation(2),
                ).toMutable().apply {
                    addInstructions(
                        """
                            iget-object v0, p0, $definingClass->$urlFieldName:Ljava/lang/String;
                            return-object v0
                            """,
                    )
                }
            )
        }
    }
}

/**
 * @param highPriority If the hook should be called before all other hooks.
 */
internal fun addImageUrlHook(
    targetMethodClass: String = EXTENSION_SHARED_CLASS_DESCRIPTOR,
    highPriority: Boolean = true
) {
    loadImageUrlMethod.addInstructions(
        if (highPriority) 0 else loadImageUrlIndex,
        """
                invoke-static { p1 }, $targetMethodClass->overrideImageURL(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p1
                """,
    )
    loadImageUrlIndex += 2
}

/**
 * If a connection completed, which includes normal 200 responses but also includes
 * status 404 and other error like http responses.
 */
internal fun addImageUrlSuccessCallbackHook(targetMethodClass: String) {
    loadImageSuccessCallbackMethod.addInstruction(
        loadImageSuccessCallbackIndex++,
        "invoke-static { p1, p2 }, $targetMethodClass->handleCronetSuccess(" +
                "Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;)V",
    )
}

/**
 * If a connection outright failed to complete any connection.
 */
internal fun addImageUrlErrorCallbackHook(targetMethodClass: String) {
    loadImageErrorCallbackMethod.addInstruction(
        loadImageErrorCallbackIndex++,
        "invoke-static { p1, p2, p3 }, $targetMethodClass->handleCronetFailure(" +
                "Lorg/chromium/net/UrlRequest;Lorg/chromium/net/UrlResponseInfo;Ljava/io/IOException;)V",
    )
}

