package app.revanced.util.patch

import app.revanced.patcher.PatchClass
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch

abstract class BaseBytecodePatch(
    name: String? = null,
    description: String? = null,
    dependencies: Set<PatchClass>? = null,
    compatiblePackages: Set<CompatiblePackage>? = null,
    fingerprints: Set<MethodFingerprint> = emptySet(),
    requiresIntegrations: Boolean = false,
    use: Boolean = true,
) : BytecodePatch(
    name = name,
    description = description,
    dependencies = dependencies,
    compatiblePackages = compatiblePackages,
    fingerprints = fingerprints,
    requiresIntegrations = requiresIntegrations,
    use = use
)