package app.revanced.patches.youtube.misc.test

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.misc.test.fingerprints.ClientNamelEnumConstructorFingerprint
import app.revanced.patches.youtube.misc.test.fingerprints.OverrideBuildVersionFingerprint
import app.revanced.patches.youtube.misc.test.fingerprints.SetClientNameFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.getWalkerMethod
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
object SpoofTestClientPatch : BaseBytecodePatch(
    name = "Spoof test client",
    description = "Adds an option to spoof as test client.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        ClientNamelEnumConstructorFingerprint,
        OverrideBuildVersionFingerprint,
        SetClientNameFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/SpoofTestClientPatch;"

    override fun execute(context: BytecodeContext) {
        var clientNameEnumClass: String
        var testClientEnumReference: String

        // region get test client enum and reference

        ClientNamelEnumConstructorFingerprint.resultOrThrow().mutableMethod.apply {
            clientNameEnumClass = definingClass

            val testClientStringIndex = getStringInstructionIndex("ANDROID_TESTSUITE")
            val testClientEnumIndex = getTargetIndex(testClientStringIndex, Opcode.SPUT_OBJECT)
            testClientEnumReference = getInstruction<ReferenceInstruction>(testClientEnumIndex).reference.toString()
        }

        // endregion

        // region override client name

        SetClientNameFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val walkerIndex = it.scanResult.patternScanResult!!.startIndex + 2
                val walkerReference = getInstruction<ReferenceInstruction>(walkerIndex).reference as MethodReference
                if (walkerReference.parameterTypes[2].toString() != clientNameEnumClass)
                    throw PatchException("parameterType does not match")

                val walkerMethod = getWalkerMethod(context, walkerIndex)
                walkerMethod.apply {
                    addInstructionsWithLabels(
                        0, """
                            invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->spoofTestClient()Z
                            move-result v0
                            if-eqz v0, :ignore
                            sget-object p2, $testClientEnumReference
                            """, ExternalLabel("ignore", getInstruction(0))
                    )
                }
            }
        }

        // endregion

        // region override client version

        OverrideBuildVersionFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex + 1
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex + 1).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->spoofTestClient(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$insertRegister
                        """
                )
            }
        }

        // endregion

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_CATEGORY: MISC_EXPERIMENTAL_FLAGS",
                "SETTINGS: SPOOF_TEST_CLIENT"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}