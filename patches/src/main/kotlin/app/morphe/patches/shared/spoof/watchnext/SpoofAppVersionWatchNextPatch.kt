package app.morphe.patches.shared.spoof.watchnext

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.clientTypeFingerprint
import app.morphe.patches.shared.createPlayerRequestBodyFingerprint
import app.morphe.patches.shared.indexOfClientInfoInstruction
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

fun spoofAppVersionWatchNextPatch(
    block: BytecodePatchBuilder.() -> Unit = {},
    patchRequired: BytecodePatchBuilder.() -> Boolean = { false },
    availabilityDescriptor: String,
    appVersionDescriptor: String,
    executeBlock: BytecodePatchContext.() -> Unit = {},
) = bytecodePatch(
    description = "spoofAppVersionWatchNextPatch",
) {
    block()

    execute {
        if (patchRequired()) {
            fun MutableMethod.getReference(index: Int) =
                getInstruction<ReferenceInstruction>(index).reference

            fun MutableMethod.getFieldReference(index: Int) =
                getReference(index) as FieldReference

            val (clientInfoClass, clientInfoReference, clientVersionReference) =
                clientTypeFingerprint.matchOrThrow().let {
                    with(it.method) {
                        val clientInfoIndex = indexOfClientInfoInstruction(this)
                        val dummyClientVersionIndex = it.stringMatches!!.first().index
                        val dummyClientVersionRegister =
                            getInstruction<OneRegisterInstruction>(dummyClientVersionIndex).registerA
                        val clientVersionIndex =
                            indexOfFirstInstructionOrThrow(dummyClientVersionIndex) {
                                opcode == Opcode.IPUT_OBJECT &&
                                        getReference<FieldReference>()?.type == "Ljava/lang/String;" &&
                                        (this as TwoRegisterInstruction).registerA == dummyClientVersionRegister
                            }

                        val clientInfoReference = getFieldReference(clientInfoIndex)
                        val clientInfoClass = clientInfoReference.definingClass

                        Triple(
                            clientInfoClass,
                            clientInfoReference,
                            getFieldReference(clientVersionIndex)
                        )
                    }
                }

            fun getSmaliInstructions(isStatic: Boolean): String {
                val clientInfoParameter = if (isStatic) "p0" else "p1"
                return """
                    invoke-static { }, $availabilityDescriptor
                    move-result v0
                    if-eqz v0, :disabled
                    invoke-static { }, $appVersionDescriptor
                    move-result-object v0
                    iget-object v1, $clientInfoParameter, $clientInfoReference
                    iput-object v0, v1, $clientVersionReference
                    :disabled
                    return-void
                    """
            }

            // region patch for spoof client body for the '/get_watch' endpoint.

            createPlayerRequestBodyFingerprint.matchOrThrow().let {
                it.method.apply {
                    val helperMethodName = "setClientInfo"
                    val checkCastIndex = it.instructionMatches.first().index

                    val checkCastInstruction =
                        getInstruction<OneRegisterInstruction>(checkCastIndex)
                    val requestMessageInstanceRegister = checkCastInstruction.registerA
                    val clientInfoContainerClassName =
                        checkCastInstruction.getReference<TypeReference>()!!.type

                    addInstruction(
                        checkCastIndex + 1,
                        "invoke-static { v$requestMessageInstanceRegister }, " +
                                "$definingClass->$helperMethodName($clientInfoContainerClassName)V",
                    )

                    // Change client info to use the spoofed values.
                    // Do this in a helper method, to remove the need of picking out multiple free registers from the hooked code.
                    it.classDef.methods.add(
                        ImmutableMethod(
                            definingClass,
                            helperMethodName,
                            listOf(
                                ImmutableMethodParameter(
                                    clientInfoContainerClassName,
                                    annotations,
                                    "clientInfoContainer"
                                )
                            ),
                            "V",
                            AccessFlags.PRIVATE or AccessFlags.STATIC,
                            annotations,
                            null,
                            MutableMethodImplementation(4),
                        ).toMutable().apply {
                            addInstructionsWithLabels(
                                0,
                                getSmaliInstructions(true),
                            )
                        },
                    )
                }
            }

            // endregion.

            // region patch for spoof client body for the '/next' endpoint.

            val watchNextResponseModelClass =
                with(watchNextResponseModelClassResolverFingerprint.methodOrThrow()) {
                    val listenableFutureIndex =
                        indexOfListenableFutureReference(this)
                    val watchNextResponseModelClassIndex =
                        indexOfFirstInstructionOrThrow(listenableFutureIndex, Opcode.CHECK_CAST)

                    getReference(watchNextResponseModelClassIndex).toString()
                }

            val watchNextSyntheticFingerprint = legacyFingerprint(
                name = "watchNextSyntheticFingerprint",
                returnType = "Ljava/lang/Object;",
                parameters = listOf("Lcom/google/protobuf/MessageLite;"),
                opcodes = listOf(
                    Opcode.CHECK_CAST,
                    Opcode.NEW_INSTANCE,
                    Opcode.INVOKE_DIRECT,
                    Opcode.RETURN_OBJECT,
                ),
                customFingerprint = custom@{ method, classDef ->
                    if (classDef.methods.count() != 2) return@custom false
                    val methodAccessFlags = method.accessFlags
                    // 'public final bridge synthetic' or 'public final synthetic'
                    if (!AccessFlags.SYNTHETIC.isSet(methodAccessFlags)) return@custom false
                    if (!AccessFlags.FINAL.isSet(methodAccessFlags)) return@custom false
                    if (!AccessFlags.PUBLIC.isSet(methodAccessFlags)) return@custom false

                    method.indexOfFirstInstructionReversed {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_DIRECT &&
                                reference?.definingClass == watchNextResponseModelClass &&
                                reference.returnType == "V" &&
                                reference.name == "<init>" &&
                                reference.parameterTypes.size == 1
                    } >= 0
                }
            )

            val syntheticClass = watchNextConstructorFingerprint.matchOrThrow(
                watchNextSyntheticFingerprint
            ).let { result ->
                with(result.method) {
                    val directIndex = result.instructionMatches.first().index
                    val startRegister =
                        getInstruction<RegisterRangeInstruction>(directIndex).startRegister
                    val directReference =
                        getReference(directIndex) as MethodReference
                    val messageIndex =
                        directReference.parameterTypes.indexOfFirst { it == "Lcom/google/protobuf/MessageLite;" }
                    val targetRegister = startRegister + messageIndex + 1 + 2

                    val targetIndex = indexOfFirstInstructionReversedOrThrow(directIndex) {
                        (opcode == Opcode.SGET_OBJECT || opcode == Opcode.NEW_INSTANCE) &&
                                (this as OneRegisterInstruction).registerA == targetRegister
                    }
                    val targetReference = getReference(targetIndex)

                    val syntheticClass = when (targetReference) {
                        is FieldReference -> {
                            targetReference.type
                        }

                        is TypeReference -> {
                            targetReference.type
                        }

                        else -> {
                            throw PatchException("synthetic class not found.")
                        }
                    }
                    syntheticClass
                }
            }

            fun indexOfClientMessageInstruction(method: Method) =
                method.indexOfFirstInstruction {
                    val reference = getReference<FieldReference>()
                    opcode == Opcode.IPUT_OBJECT &&
                            reference?.type == clientInfoClass &&
                            reference.name == "d"
                }

            val clientMessageFingerprint = legacyFingerprint(
                name = "clientMessageFingerprint",
                returnType = "Ljava/lang/Object;",
                accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
                customFingerprint = { method, classDef ->
                    classDef.type == syntheticClass &&
                            indexOfClientMessageInstruction(method) >= 0
                },
            )

            clientMessageFingerprint.matchOrThrow().let {
                it.method.apply {
                    val helperMethodName = "patch_setClientVersion"

                    val insertIndex = indexOfClientMessageInstruction(this)
                    val messageRegister =
                        getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                    addInstruction(
                        insertIndex,
                        "invoke-direct { p0, v$messageRegister }, $definingClass->$helperMethodName($clientInfoClass)V"
                    )

                    it.classDef.methods.add(
                        ImmutableMethod(
                            definingClass,
                            helperMethodName,
                            listOf(
                                ImmutableMethodParameter(
                                    clientInfoClass,
                                    annotations,
                                    "clientInfoClass"
                                )
                            ),
                            "V",
                            AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                            annotations,
                            null,
                            MutableMethodImplementation(4),
                        ).toMutable().apply {
                            addInstructionsWithLabels(
                                0,
                                getSmaliInstructions(false),
                            )
                        },
                    )
                }
            }

            // endregion.

        }

        executeBlock()
    }
}
