package app.revanced.patches.youtube.button.whitelist.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.MethodFingerprintExtensions.name
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.youtube.button.whitelist.fingerprint.*
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodImplementation
import org.jf.dexlib2.Opcode

@Name("channel-whitelist")
@YouTubeCompatibility
@Version("0.0.1")
class WhitelistPatch : BytecodePatch(
    listOf(
        PlayerResponseModelFingerprint,
        PlayerResponseModelParentFingerprint,
        PrimaryInjectFingerprint,
        SecondaryInjectFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val PlayerResponseModelParentResult = PlayerResponseModelParentFingerprint.result!!
        val PlayerResponseModelParentInstructions = PlayerResponseModelParentResult.mutableMethod.implementation!!.instructions

        val injectIndex = PlayerResponseModelParentInstructions.indexOfFirst {
            it.opcode == Opcode.CONST_STRING &&
            (it as BuilderInstruction21c).reference.toString() == "Person"
        } + 2

        val PlayerResponseModelReference =
            PlayerResponseModelParentResult.method.let { method ->
                (method.implementation!!.instructions.elementAt(injectIndex) as ReferenceInstruction).reference as DexBackedMethodReference
            }

        val PlayerResponseModelResult = PlayerResponseModelFingerprint.result!!

        val PrimaryReference =
            PlayerResponseModelResult.method.let { method ->
                (method.implementation!!.instructions.elementAt(2) as ReferenceInstruction).reference as FieldReference
            }
        val SecondaryReference =
            PlayerResponseModelResult.method.let { method ->
                (method.implementation!!.instructions.elementAt(3) as ReferenceInstruction).reference as FieldReference
            }
        val TertiaryReference =
            PlayerResponseModelResult.method.let { method ->
                (method.implementation!!.instructions.elementAt(4) as ReferenceInstruction).reference as MethodReference
            }

        val classDef = PlayerResponseModelResult.mutableClass 
        classDef.methods.add(
            ImmutableMethod(
                classDef.type,
                "setCurrentVideoInformation",
                listOf(),
                "V",
                AccessFlags.PRIVATE or AccessFlags.FINAL,
                null,
                null,
                ImmutableMethodImplementation(
                    2, """
                        iget-object v0, v1, ${PlayerResponseModelResult.classDef.type}->${PrimaryReference.name}:${PrimaryReference.type}
                        iget-object v0, v0, ${PrimaryReference.type}->${SecondaryReference.name}:${SecondaryReference.type}
                        invoke-interface {v0}, $TertiaryReference
                        move-result-object v0
                        invoke-interface {v0}, $PlayerResponseModelReference
                        move-result-object v0
                        invoke-static {v0}, $VIDEO_PATH/VideoInformation;->setChannelName(Ljava/lang/String;)V
                        return-void
                    """.toInstructions(), null, null
                )
            ).toMutable()
        )


        listOf(
            PrimaryInjectFingerprint,
            SecondaryInjectFingerprint
        ).forEach { fingerprint ->
            val result = fingerprint.result ?: return PatchResultError("${fingerprint.name} not found")
            val method = result.mutableMethod
            val index = result.scanResult.patternScanResult!!.endIndex + 1
            method.addInstruction(
                index,
                "invoke-direct {p0}, ${PlayerResponseModelResult.classDef.type}->setCurrentVideoInformation()V"
            )
        }

        return PatchResultSuccess()
    }
}
