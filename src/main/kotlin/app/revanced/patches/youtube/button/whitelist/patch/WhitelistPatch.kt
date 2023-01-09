package app.revanced.patches.youtube.button.whitelist.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.youtube.button.whitelist.fingerprint.*
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.toErrorResult
import app.revanced.shared.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodImplementation

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

        PlayerResponseModelParentFingerprint.result?.mutableMethod?.let { method ->
            val instructions = method.implementation!!.instructions
            val injectIndex = instructions.indexOfFirst {
                it.opcode == Opcode.CONST_STRING &&
                        (it as BuilderInstruction21c).reference.toString() == "Person"
            } + 2
            fourthRef = (instructions.elementAt(injectIndex) as ReferenceInstruction).reference as DexBackedMethodReference
        } ?: return PlayerResponseModelParentFingerprint.toErrorResult()

        PlayerResponseModelFingerprint.result?.let { result ->

            with (result.method.implementation!!.instructions) {
                firstRef = (elementAt(2) as ReferenceInstruction).reference as FieldReference
                secondRef = (elementAt(3) as ReferenceInstruction).reference as FieldReference
                thirdRef = (elementAt(4) as ReferenceInstruction).reference as MethodReference
            }

            with (result.mutableClass) {
                methods.add(
                    ImmutableMethod(
                        type,
                        "setCurrentVideoInformation",
                        listOf(),
                        "V",
                        AccessFlags.PRIVATE or AccessFlags.FINAL,
                        null,
                        null,
                        ImmutableMethodImplementation(
                            2, """
                        iget-object v0, v1, ${result.classDef.type}->${firstRef.name}:${firstRef.type}
                        iget-object v0, v0, ${firstRef.type}->${secondRef.name}:${secondRef.type}
                        invoke-interface {v0}, $thirdRef
                        move-result-object v0
                        invoke-interface {v0}, $fourthRef
                        move-result-object v0
                        invoke-static {v0}, $VIDEO_PATH/VideoInformation;->setChannelName(Ljava/lang/String;)V
                        return-void
                    """.toInstructions(), null, null
                        )
                    ).toMutable()
                )
            }

            listOf(
                PrimaryInjectFingerprint,
                SecondaryInjectFingerprint
            ).map {
                it.result ?: return it.toErrorResult()
            }.forEach {
                val method = it.mutableMethod
                val index = it.scanResult.patternScanResult!!.endIndex + 1
                method.addInstruction(
                    index,
                    "invoke-direct {p0}, ${result.classDef.type}->setCurrentVideoInformation()V"
                )
            }
        } ?: return PlayerResponseModelFingerprint.toErrorResult()

        val PlayerResponseModelResult = PlayerResponseModelFingerprint.result!!

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
                        iget-object v0, v1, ${PlayerResponseModelResult.classDef.type}->${firstRef.name}:${firstRef.type}
                        iget-object v0, v0, ${firstRef.type}->${secondRef.name}:${secondRef.type}
                        invoke-interface {v0}, $thirdRef
                        move-result-object v0
                        invoke-interface {v0}, $fourthRef
                        move-result-object v0
                        invoke-static {v0}, $VIDEO_PATH/VideoInformation;->setChannelName(Ljava/lang/String;)V
                        return-void
                    """.toInstructions(), null, null
                )
            ).toMutable()
        )

        return PatchResultSuccess()
    }

    companion object {
        private lateinit var firstRef: FieldReference
        private lateinit var secondRef: FieldReference
        private lateinit var thirdRef: MethodReference
        private lateinit var fourthRef: DexBackedMethodReference
    }
}
