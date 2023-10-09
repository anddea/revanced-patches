package app.revanced.patches.youtube.misc.forcevp9

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.misc.forcevp9.fingerprints.VideoCapabilitiesFingerprint
import app.revanced.patches.youtube.misc.forcevp9.fingerprints.VideoCapabilitiesParentFingerprint
import app.revanced.patches.youtube.misc.forcevp9.fingerprints.Vp9PrimaryFingerprint
import app.revanced.patches.youtube.misc.forcevp9.fingerprints.Vp9PropsFingerprint
import app.revanced.patches.youtube.misc.forcevp9.fingerprints.Vp9PropsParentFingerprint
import app.revanced.patches.youtube.misc.forcevp9.fingerprints.Vp9SecondaryFingerprint
import app.revanced.patches.youtube.utils.fingerprints.LayoutSwitchFingerprint
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.dexbacked.reference.DexBackedFieldReference
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch(
    name = "Force VP9 codec",
    description = "Forces the VP9 codec for videos.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.22.37",
                "18.23.36",
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40"
            ]
        )
    ]
)
@Suppress("unused")
object ForceVP9CodecPatch : BytecodePatch(
    setOf(
        LayoutSwitchFingerprint,
        VideoCapabilitiesParentFingerprint,
        Vp9PropsParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        LayoutSwitchFingerprint.result?.classDef?.let { classDef ->
            arrayOf(
                Vp9PrimaryFingerprint,
                Vp9SecondaryFingerprint
            ).forEach { fingerprint ->
                fingerprint.also { it.resolve(context, classDef) }.result?.injectOverride()
                    ?: throw fingerprint.exception
            }
        } ?: throw LayoutSwitchFingerprint.exception

        Vp9PropsParentFingerprint.result?.let { parentResult ->
            Vp9PropsFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.mutableMethod?.let {
                mapOf(
                    "MANUFACTURER" to "getManufacturer",
                    "BRAND" to "getBrand",
                    "MODEL" to "getModel"
                ).forEach { (fieldName, descriptor) ->
                    it.hookProps(fieldName, descriptor)
                }
            } ?: throw Vp9PropsFingerprint.exception
        } ?: throw Vp9PropsParentFingerprint.exception

        VideoCapabilitiesParentFingerprint.result?.let { parentResult ->
            VideoCapabilitiesFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.startIndex

                    addInstructions(
                        insertIndex, """
                            invoke-static {p1}, $INTEGRATIONS_CLASS_DESCRIPTOR->overrideMinHeight(I)I
                            move-result p1
                            invoke-static {p2}, $INTEGRATIONS_CLASS_DESCRIPTOR->overrideMaxHeight(I)I
                            move-result p2
                            invoke-static {p3}, $INTEGRATIONS_CLASS_DESCRIPTOR->overrideMinWidth(I)I
                            move-result p3
                            invoke-static {p4}, $INTEGRATIONS_CLASS_DESCRIPTOR->overrideMaxWidth(I)I
                            move-result p4
                            """
                    )
                }
            } ?: throw VideoCapabilitiesFingerprint.exception
        } ?: throw VideoCapabilitiesParentFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_VP9_CODEC"
            )
        )

        SettingsPatch.updatePatchStatus("force-vp9-codec")

    }

    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/CodecOverridePatch;"

    private const val INTEGRATIONS_CLASS_METHOD_REFERENCE =
        "$INTEGRATIONS_CLASS_DESCRIPTOR->shouldForceVP9(Z)Z"

    private fun MethodFingerprintResult.injectOverride() {
        mutableMethod.apply {
            val startIndex = scanResult.patternScanResult!!.startIndex
            val endIndex = scanResult.patternScanResult!!.endIndex

            val startRegister = getInstruction<OneRegisterInstruction>(startIndex).registerA
            val endRegister = getInstruction<OneRegisterInstruction>(endIndex).registerA

            hookOverride(endIndex + 1, endRegister)
            removeInstruction(endIndex)
            hookOverride(startIndex + 1, startRegister)
            removeInstruction(startIndex)
        }
    }

    private fun MutableMethod.hookOverride(
        index: Int,
        register: Int
    ) {
        addInstructions(
            index, """
                    invoke-static {v$register}, $INTEGRATIONS_CLASS_METHOD_REFERENCE
                    move-result v$register
                    return v$register
                    """
        )
    }

    private fun MutableMethod.hookProps(
        fieldName: String,
        descriptor: String
    ) {
        val targetString = "Landroid/os/Build;->" +
                fieldName +
                ":Ljava/lang/String;"

        for ((index, instruction) in implementation!!.instructions.withIndex()) {
            if (instruction.opcode != Opcode.SGET_OBJECT) continue

            val indexString =
                ((instruction as? ReferenceInstruction)?.reference as? DexBackedFieldReference).toString()

            if (indexString != targetString) continue

            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstructions(
                index + 1, """
                        invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR->$descriptor(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$register
                        """
            )
            break
        }
    }

}
