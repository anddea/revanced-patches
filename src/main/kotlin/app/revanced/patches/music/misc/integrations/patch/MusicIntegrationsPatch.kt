package app.revanced.patches.music.misc.integrations.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patches.music.misc.integrations.fingerprints.InitFingerprint
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.patches.integrations.AbstractIntegrationsPatch
import app.revanced.shared.util.integrations.Constants.MUSIC_SETTINGS_PATH

@Name("music-integrations")
@YouTubeMusicCompatibility
class MusicIntegrationsPatch : AbstractIntegrationsPatch(
    "$MUSIC_SETTINGS_PATH",
    listOf(InitFingerprint),
)