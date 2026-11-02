// SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
// SPDX-License-Identifier: AGPL-3.0-or-later

package app.morphe.patches.youtube.interaction.queue

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.misc.fix.proto.fixProtoLibraryPatch
import app.morphe.patches.shared.misc.fix.proto.parseByteArrayWithRegistryMethodRef
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.UNLOCK_QUEUE
import app.morphe.patches.youtube.utils.playservice.is_20_21_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.util.Utils.printWarn
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.cloneMutable
import app.morphe.util.cloneMutableAndPreserveParameters
import app.morphe.util.getFreeRegisterProvider
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION = "Lapp/morphe/extension/youtube/patches/queue/QueuePatch;"
private const val MESSAGE = "Lcom/google/protobuf/MessageLite;"
private const val FUTURE = "Lcom/google/common/util/concurrent/ListenableFuture;"
private const val OPTIONAL = "Lj$/util/Optional;"

@Suppress("unused")
val unlockQueuePatch = bytecodePatch(
    UNLOCK_QUEUE.title,
    UNLOCK_QUEUE.summary
) {
    dependsOn(sharedExtensionPatch, settingsPatch, videoInformationPatch, fixProtoLibraryPatch, versionCheckPatch)
    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        if (!is_20_21_or_greater) {
            printWarn("\"Unlock queue\" is not supported in this version. Use YouTube 20.21 or greater.")
            return@execute
        }

        addPreference(arrayOf("PREFERENCE_SCREEN: VIDEO", "SETTINGS: UNLOCK_QUEUE"), UNLOCK_QUEUE)

        // Commands contain extensions, so parse with the generated registry, as upstream does.
        val parser = parseByteArrayWithRegistryMethodRef.get()!!
        val extensionMethods = mutableClassDefBy(EXTENSION).methods
        val parseStub = extensionMethods.single { it.name == "parse" }
        val parseBridge = parseStub.cloneMutable(registerCount = 3, clearImplementation = true)
        extensionMethods.remove(parseStub)
        extensionMethods.add(parseBridge)
        parseBridge.addInstructions(
            0,
            """
                check-cast p0, ${parser.parameterTypes.first()}
                invoke-static {}, Lcom/google/protobuf/ExtensionRegistryLite;->getGeneratedRegistry()Lcom/google/protobuf/ExtensionRegistryLite;
                move-result-object v0
                invoke-static {p0, p1, v0}, $parser
                move-result-object p0
                return-object p0
            """
        )

        MenuCommandResolverFingerprint.method.apply {
            val command = parameterTypes.first()
            addInstructions(
                0,
                """
                    invoke-static/range {p1 .. p1}, $EXTENSION->unlockMenu($MESSAGE)$MESSAGE
                    move-result-object p1
                    check-cast p1, $command
                """
            )
        }

        val senderFingerprint = Fingerprint(
            returnType = FUTURE,
            custom = { method, _ -> method.parameterTypes.contains("Ljava/lang/String;") },
            filters = listOf(methodCall(smali = ActiveDevicesRequestFingerprint.method.toString()))
        )
        val sender = senderFingerprint.method
        // Older releases send only the device ID.
        if (sender.parameterTypes.size > 1) {
            // Keep the request params intact even if the app reuses their original register.
            sender.cloneMutableAndPreserveParameters(senderFingerprint.classDef).apply {
                val responseIndex = instructions.indexOfFirst {
                    it.opcode == Opcode.INVOKE_VIRTUAL && it.getReference<MethodReference>()?.returnType == FUTURE
                } + 1
                check(responseIndex > 0 && getInstruction(responseIndex).opcode == Opcode.MOVE_RESULT_OBJECT)
                val response = getInstruction<OneRegisterInstruction>(responseIndex).registerA
                val free = getFreeRegisterProvider(responseIndex + 1, 2, response)
                val first = free.getFreeRegister4Bit()
                val second = free.getFreeRegister4Bit()
                addInstructions(
                    responseIndex + 1,
                    """
                        move-object/from16 v$first, v$response
                        move-object/from16 v$second, p2
                        invoke-static {v$first, v$second}, $EXTENSION->wrap(${FUTURE}Ljava/lang/String;)$FUTURE
                        move-result-object v$response
                    """
                )
            }
        }

        val resolver = QueueInsertResolverFingerprint.method
        val contextIndex = resolver.instructions.indexOfFirst {
            it.opcode == Opcode.IGET_OBJECT && it.getReference<FieldReference>()?.type == OPTIONAL
        }
        check(contextIndex >= 0) { "Queue context field not found" }
        val context = resolver.getInstruction(contextIndex).getReference<FieldReference>()!!
        // Older releases merge resolvers and keep the queue state in an Object field.
        val stateField = resolver.instructions.take(contextIndex).last {
            it.opcode == Opcode.IGET_OBJECT && it.getReference<FieldReference>()?.definingClass == resolver.definingClass
        }.getReference<FieldReference>()!!
        val state = mutableClassDefBy(context.definingClass)
        val update = Fingerprint(
            definingClass = state.type,
            returnType = "V",
            custom = { method, _ -> method.parameterTypes.size == 1 },
            filters = listOf(methodCall(parameters = emptyList(), returnType = "I"))
        ).method
        val watchModel = mutableClassDefBy(update.parameterTypes.single().toString())
        val watchInit = watchModel.methods.single { it.name == "<init>" && it.parameterTypes.size == 1 }
        val watchedField = watchModel.fields.single { it.type == resolver.parameterTypes.first() }
        watchInit.instructions.withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }.reversed().forEach { (index, _) ->
            val free = watchInit.getFreeRegisterProvider(index, 1).getFreeRegister4Bit()
            watchInit.addInstructionsAtControlFlowLabel(
                index,
                """
                    move-object/from16 v$free, p0
                    iget-object v$free, v$free, $watchedField
                    invoke-static {v$free}, $EXTENSION->watched($MESSAGE)V
                """
            )
        }

        val byteString = classDefBy(MESSAGE).methods.single { it.name == "toByteString" }.returnType
        val byteStringClass = mutableClassDefBy(byteString)
        val bytes = byteStringClass.methods.single { it.parameterTypes.isEmpty() && it.returnType == "[B" }
        val copy = byteStringClass.methods.single { method ->
            AccessFlags.STATIC.isSet(method.accessFlags) && method.parameterTypes == listOf("[B") &&
                    method.returnType == byteString && method.instructions.any {
                it.getReference<MethodReference>()?.parameterTypes == listOf("[B", "I", "I")
            }
        }
        // A small helper keeps all temporary registers local, including on older Object-backed state.
        val complete = ImmutableMethod(
            state.type, "patch_completeQueueContext",
            listOf(ImmutableMethodParameter("Ljava/lang/Object;", null, null)), "V",
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value, null, null,
            MutableMethodImplementation(4)
        ).toMutable().apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $EXTENSION->isEnabled()Z
                    move-result v0
                    if-eqz v0, :done
                    instance-of v0, p0, ${state.type}
                    if-eqz v0, :done
                    check-cast p0, ${state.type}
                    iget-object v0, p0, $context
                    const/4 v1, 0x0
                    invoke-virtual {v0}, $OPTIONAL->isPresent()Z
                    move-result v2
                    if-eqz v2, :complete
                    invoke-virtual {v0}, $OPTIONAL->get()Ljava/lang/Object;
                    move-result-object v1
                    check-cast v1, $byteString
                    invoke-virtual {v1}, $bytes
                    move-result-object v1
                    :complete
                    invoke-static {v1}, $EXTENSION->complete([B)[B
                    move-result-object v1
                    if-eqz v1, :done
                    invoke-static {v1}, $copy
                    move-result-object v1
                    invoke-static {v1}, $OPTIONAL->of(Ljava/lang/Object;)$OPTIONAL
                    move-result-object v1
                    iput-object v1, p0, $context
                    :done
                    return-void
                """
            )
        }
        state.methods.add(complete)
        val free = resolver.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()
        resolver.addInstructions(
            0,
            """
                move-object/from16 v$free, p0
                iget-object v$free, v$free, $stateField
                invoke-static {v$free}, $complete
            """
        )
    }
}
