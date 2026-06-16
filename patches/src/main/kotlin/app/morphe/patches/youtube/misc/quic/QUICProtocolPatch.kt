package app.morphe.patches.youtube.misc.quic

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.quic.baseQuicProtocolPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.patch.PatchList.DISABLE_QUIC_PROTOCOL
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch

@Suppress("unused", "SpellCheckingInspection")
val quicProtocolPatch = bytecodePatch(
    DISABLE_QUIC_PROTOCOL.title,
    DISABLE_QUIC_PROTOCOL.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

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
