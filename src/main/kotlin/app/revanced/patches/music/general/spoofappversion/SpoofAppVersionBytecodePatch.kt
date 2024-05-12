package app.revanced.patches.music.general.spoofappversion

import app.revanced.patches.music.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.shared.spoofappversion.BaseSpoofAppVersionPatch

object SpoofAppVersionBytecodePatch : BaseSpoofAppVersionPatch(
    "$GENERAL_CLASS_DESCRIPTOR->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"
)