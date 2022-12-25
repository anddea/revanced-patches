package app.revanced.patches.youtube.misc.settings.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.patches.youtube.misc.settings.bytecode.fingerprints.ThemeSetterSystemFingerprint
import app.revanced.shared.extensions.findMutableMethodOf
import app.revanced.shared.extensions.injectTheme
import app.revanced.shared.patches.mapping.ResourceMappingPatch
import app.revanced.patches.youtube.misc.integrations.patch.IntegrationsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.INTEGRATIONS_PATH
import org.jf.dexlib2.iface.instruction.formats.Instruction31i
import org.jf.dexlib2.Opcode

@Name("settings-bytecode-patch")
@DependsOn(
    [
        IntegrationsPatch::class,
        ResourceMappingPatch::class,
        SharedResourcdIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SettingsBytecodePatch : BytecodePatch(
    listOf(ThemeSetterSystemFingerprint)
) {

    // list of resource names to get the id of
    private val resourceIds = arrayOf(
        "Theme.YouTube.Light",
        "Theme.YouTube.Light.DarkerPalette"
    ).map { name ->
        ResourceMappingPatch.resourceMappings.single { it.name == name }.id
    }

    override fun execute(context: BytecodeContext): PatchResult {
        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                with(method.implementation) {
                    this?.instructions?.forEachIndexed { index, instruction ->
                        when (instruction.opcode) {
                            Opcode.CONST -> {
                                when ((instruction as Instruction31i).wideLiteral) {
                                    resourceIds[0] -> { // primary theme
                                        val insertIndex = index - 3
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.IF_NE) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (instructions.elementAt(index) as Instruction31i).registerA
                                        mutableMethod.implementation!!.injectTheme(mutableMethod.implementation!!.instructions.size - 1 , viewRegister, "setPrimaryTheme")
                                    }

                                    resourceIds[1] -> { // secondary theme
                                        val insertIndex = index - 3
                                        val invokeInstruction = instructions.elementAt(insertIndex)
                                        if (invokeInstruction.opcode != Opcode.IF_NE) return@forEachIndexed

                                        val mutableMethod = context.proxy(classDef).mutableClass.findMutableMethodOf(method)

                                        val viewRegister = (instructions.elementAt(index) as Instruction31i).registerA
                                        mutableMethod.implementation!!.injectTheme(index + 2, viewRegister, "setSecondaryTheme")
                                    }
                                }
                            }
                            else -> return@forEachIndexed
                        }
                    }
                }
            }
        }

        // apply the current theme of the settings page
        with(ThemeSetterSystemFingerprint.result!!) {
            with(mutableMethod) {
                val call = "invoke-static {v0}, $INTEGRATIONS_PATH/utils/ThemeHelper;->setTheme(Ljava/lang/Object;)V"

                addInstruction(
                    scanResult.patternScanResult!!.startIndex,
                    call
                )

                addInstruction(
                    mutableMethod.implementation!!.instructions.size - 1,
                    call
                )
            }
        }
        return PatchResultSuccess()
    }
}
