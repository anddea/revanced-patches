package app.revanced.patches.youtube.utils.fix.hype

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.shared.clientTypeFingerprint
import app.revanced.patches.shared.createPlayerRequestBodyFingerprint
import app.revanced.patches.shared.indexOfClientInfoInstruction
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.playservice.is_19_26_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.or
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

private val hypeButtonIconResourcePatch = resourcePatch(
    description = "hypeButtonIconResourcePatch"
) {
    dependsOn(versionCheckPatch)

    execute {
        if (is_19_26_or_greater) {
            return@execute
        }
        arrayOf(
            "xxxhdpi",
            "xxhdpi",
            "xhdpi",
            "hdpi",
            "mdpi"
        ).forEach { dpi ->
            copyResources(
                "youtube/hype",
                ResourceGroup(
                    "drawable-$dpi",
                    "yt_fill_star_shooting_black_24.png",
                    "yt_outline_star_shooting_black_24.png"
                )
            )
        }
    }
}

/**
 * 1. YouTube 19.25.39 can be used without the 'Disable update screen' patch.
 *    This means that even if you use an unpatched YouTube 19.25.39, the 'Update your app' pop-up will not appear.
 * 2. Due to a server-side change, the Hype button is now available on YouTube 19.25.39 and earlier.
 * 3. Google did not add the Hype icon (R.drawable.yt_outline_star_shooting_black_24) to YouTube 19.25.39 or earlier,
 *    So no icon appears on the Hype button when using YouTube 19.25.39.
 * 4. For the same reason, the 'buttonViewModel.iconName' value in the '/next' endpoint response from YouTube 19.25.39 is also empty.
 * 5. There was an issue where the feed layout would break if the app version was spoofed to 19.26.42 via the 'Spoof app version' patch.
 * 6. As a workaround, the app version is spoofed to 19.26.42 only for the '/get_watch' and '/next' endpoints.
 */
val hypeButtonIconPatch = bytecodePatch(
    description = "hypeButtonIconPatch"
) {
    dependsOn(
        settingsPatch,
        hypeButtonIconResourcePatch,
        versionCheckPatch
    )

    execute {
        if (is_19_26_or_greater) {
            return@execute
        }

        val spoofedAppVersion = "19.26.42"

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

        // region patch for spoof client body for the '/get_watch' endpoint.

        createPlayerRequestBodyFingerprint.matchOrThrow().let {
            it.method.apply {
                val helperMethodName = "setClientInfo"
                val checkCastIndex = it.patternMatch!!.startIndex

                val checkCastInstruction = getInstruction<OneRegisterInstruction>(checkCastIndex)
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
                            """
                            invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->fixHypeButtonIconEnabled()Z
                            move-result v0
                            if-eqz v0, :disabled
                            const-string v0, "$spoofedAppVersion"
                            iget-object v1, p0, $clientInfoReference
                            iput-object v0, v1, $clientVersionReference
                            :disabled
                            return-void
                            """,
                        )
                    },
                )
            }
        }

        // endregion.

        // region patch for spoof client body for the '/next' endpoint.

        val syntheticClass = watchNextConstructorFingerprint.matchOrThrow(
            watchNextSyntheticFingerprint
        ).let { result ->
            with(result.method) {
                val directIndex = result.patternMatch!!.startIndex
                val startRegister =
                    getInstruction<RegisterRangeInstruction>(directIndex).startRegister
                val directReference =
                    getInstruction<ReferenceInstruction>(directIndex).reference as MethodReference
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
                val messageRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

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
                            """
                                invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->fixHypeButtonIconEnabled()Z
                                move-result v0
                                if-eqz v0, :disabled
                                const-string v0, "$spoofedAppVersion"
                                iget-object v1, p1, $clientInfoReference
                                iput-object v0, v1, $clientVersionReference
                                :disabled
                                return-void
                                """,
                        )
                    },
                )
            }
        }

        // endregion.

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: FIX_HYPE_BUTTON_ICON"
            )
        )
    }
}
