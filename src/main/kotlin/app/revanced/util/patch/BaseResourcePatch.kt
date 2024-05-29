package app.revanced.util.patch

import app.revanced.patcher.PatchClass
import app.revanced.patcher.patch.ResourcePatch

abstract class BaseResourcePatch(
    name: String? = null,
    description: String? = null,
    dependencies: Set<PatchClass>? = null,
    compatiblePackages: Set<CompatiblePackage>? = null,
    requiresIntegrations: Boolean = false,
    use: Boolean = true
) : ResourcePatch(
    name = name,
    description = description,
    dependencies = dependencies,
    compatiblePackages = compatiblePackages,
    requiresIntegrations = requiresIntegrations,
    use = use
)