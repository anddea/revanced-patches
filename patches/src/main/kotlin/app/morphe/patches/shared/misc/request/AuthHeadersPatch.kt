/*
 * Copyright (C) 2026 lavinhoque33
 * Licensed under the GNU General Public License v3.0.
 */
package app.morphe.patches.shared.misc.request

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.extension.Constants.EXTENSION_PATH

// Captures the app's own auth headers for requests made from the extension, e.g. fetching a whole playlist.
internal val authHeadersPatch = bytecodePatch {
    dependsOn(buildRequestPatch)
    execute {
        hookBuildRequest("$EXTENSION_PATH/innertube/utils/AuthUtils;->setRequestHeaders(Ljava/lang/String;Ljava/util/Map;)V")
    }
}
