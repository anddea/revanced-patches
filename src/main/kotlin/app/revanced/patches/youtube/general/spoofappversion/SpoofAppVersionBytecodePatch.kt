package app.revanced.patches.youtube.general.spoofappversion

import app.revanced.patches.shared.spoofappversion.BaseSpoofAppVersionPatch
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR

object SpoofAppVersionBytecodePatch : BaseSpoofAppVersionPatch(
    "$GENERAL_CLASS_DESCRIPTOR->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"
)