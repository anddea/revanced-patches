package app.revanced.patches.music.misc.integrations.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patches.music.misc.integrations.fingerprints.InitFingerprint
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.patches.integrations.AbstractIntegrationsPatch

@Name("music-integrations")
@YouTubeMusicCompatibility
class MusicIntegrationsPatch : AbstractIntegrationsPatch(
    "Lapp/revanced/integrations/settings/MusicSettings;",
    listOf(InitFingerprint),
)