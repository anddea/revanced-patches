package app.revanced.patches.youtube.misc.quic

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patches.youtube.misc.quic.fingerprints.CronetEngineBuilderFingerprint
import app.revanced.patches.youtube.misc.quic.fingerprints.ExperimentalCronetEngineBuilderFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow

@Suppress("unused")
object QUICProtocolPatch : BaseBytecodePatch(
    name = "Disable QUIC protocol",
    description = "Adds an option to disable CronetEngine's QUIC protocol.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        CronetEngineBuilderFingerprint,
        ExperimentalCronetEngineBuilderFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        arrayOf(
            CronetEngineBuilderFingerprint,
            ExperimentalCronetEngineBuilderFingerprint
        ).forEach {
            it.resultOrThrow().mutableMethod.addInstructions(
                0, """
                    invoke-static {p1}, $MISC_PATH/QUICProtocolPatch;->disableQUICProtocol(Z)Z
                    move-result p1
                    """
            )
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: DISABLE_QUIC_PROTOCOL"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}