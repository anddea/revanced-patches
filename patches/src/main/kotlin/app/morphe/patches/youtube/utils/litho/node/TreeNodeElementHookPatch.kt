/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.utils.litho.node

import app.morphe.patches.shared.litho.context.conversionContextPatch
import app.morphe.patches.shared.litho.node.createTreeNodeElementHookPatch
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch

val treeNodeElementHookPatch = createTreeNodeElementHookPatch(
    sharedExtensionPatch,
    conversionContextPatch,
    false,
    useLegacyContextRegister = { false }
)
