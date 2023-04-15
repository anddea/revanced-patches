package app.revanced.patches.music.misc.integrations.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.patch.annotations.RequiresIntegrations
import app.revanced.patches.music.misc.integrations.fingerprints.InitFingerprint
import app.revanced.patches.shared.patch.integrations.AbstractIntegrationsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility

@Name("music-integrations")
@YouTubeMusicCompatibility
@RequiresIntegrations
class MusicIntegrationsPatch : AbstractIntegrationsPatch(
    listOf(InitFingerprint),
)