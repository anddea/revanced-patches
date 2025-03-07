package app.revanced.patches.youtube.misc.quic

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.quic.baseQuicProtocolPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.DISABLE_QUIC_PROTOCOL
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch

@Suppress("unused", "SpellCheckingInspection")
val quicProtocolPatch = bytecodePatch(
    DISABLE_QUIC_PROTOCOL.title,
    DISABLE_QUIC_PROTOCOL.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        baseQuicProtocolPatch(),
    )

    execute {
        addPreference(
            arrayOf(
                "SETTINGS: DISABLE_QUIC_PROTOCOL"
            ),
            DISABLE_QUIC_PROTOCOL
        )
    }
}
