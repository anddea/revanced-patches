package app.revanced.patches.music.utils.integrations.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.patch.annotations.RequiresIntegrations
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.integrations.fingerprints.InitFingerprint
import app.revanced.patches.shared.patch.integrations.AbstractIntegrationsPatch
import app.revanced.util.integrations.Constants.MUSIC_INTEGRATIONS_PATH

@Name("music-integrations")
@MusicCompatibility
@RequiresIntegrations
class MusicIntegrationsPatch : AbstractIntegrationsPatch(
    "$MUSIC_INTEGRATIONS_PATH/utils/ReVancedUtils;",
    listOf(InitFingerprint),
)