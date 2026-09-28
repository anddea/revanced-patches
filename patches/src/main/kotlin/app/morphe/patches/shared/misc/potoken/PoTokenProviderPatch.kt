/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2618
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.shared.misc.potoken

import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.extension.Constants.EXTENSION_PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.registersUsed
import app.morphe.util.updatePatchStatus
import org.w3c.dom.Element

private const val EXTENSION_CLASS = "Lapp/morphe/extension/shared/patches/PoTokenProviderPatch;"
private const val LOCAL_PO_TOKEN_SERVICE_ACTION = "app.morphe.extension.potokens.service.START"
private const val LOCAL_PO_TOKEN_PROVIDER_CLASS = "app.morphe.extension.shared.patches.PoTokenServiceProvider"
private const val LOCAL_PO_TOKEN_SERVICE_CLASS = "app.morphe.extension.shared.patches.PoTokenService"
private const val LOCAL_PO_TOKEN_PROVIDER_AUTHORITY_SUFFIX = ".morphe.potoken.chimera"

private lateinit var resourceContext: ResourcePatchContext

private val poTokenProviderResourcePatch = resourcePatch {
    execute {
        resourceContext = this
    }
}

internal fun poTokenProviderPatch(
    name: String,
    description: String,
    block: BytecodePatchBuilder.() -> Unit,
    executeBlock: BytecodePatchContext.() -> Unit = {},
) = bytecodePatch(
    name = name,
    description = description
) {
    block()

    dependsOn(poTokenProviderResourcePatch)

    execute {
        resourceContext.document("AndroidManifest.xml").use { document ->
            val manifestNode = document.documentElement as Element
            val applicationPackage = manifestNode.getAttribute("package")
            require(applicationPackage.isNotEmpty()) {
                "Could not determine the patched application's package name"
            }

            val applicationNode = document.getElementsByTagName("application").item(0)
            requireNotNull(applicationNode) {
                "Could not find the patched application's manifest node"
            }

            applicationNode.appendChild(document.createElement("provider").apply {
                setAttribute("android:name", LOCAL_PO_TOKEN_PROVIDER_CLASS)
                setAttribute(
                    "android:authorities",
                    applicationPackage + LOCAL_PO_TOKEN_PROVIDER_AUTHORITY_SUFFIX
                )
                setAttribute("android:exported", "true")
            })

            applicationNode.appendChild(document.createElement("service").apply {
                setAttribute("android:name", LOCAL_PO_TOKEN_SERVICE_CLASS)
                setAttribute("android:exported", "true")

                appendChild(document.createElement("intent-filter").apply {
                    appendChild(document.createElement("action").apply {
                        setAttribute("android:name", LOCAL_PO_TOKEN_SERVICE_ACTION)
                    })
                })
            })

        }

        val serviceBindMatch = ServiceBindIntentUtilsFingerprint.matchOrNull()
        if (serviceBindMatch != null) {
            serviceBindMatch.method.apply {
                val serviceActionMatch = serviceBindMatch.instructionMatches[1]
                val serviceActionIndex = serviceActionMatch.index
                val serviceActionRegister = serviceActionMatch.instruction.registersUsed[2]

                val authoritiesMatch = serviceBindMatch.instructionMatches[2]
                val authoritiesIndex = authoritiesMatch.index
                val authoritiesRegister = authoritiesMatch.instruction.registersUsed[1]

                addInstructionsAtControlFlowLabel(
                    authoritiesIndex,
                    """
                        invoke-static { v$serviceActionRegister, v$authoritiesRegister }, $EXTENSION_CLASS->overrideAuthorities(Ljava/lang/String;Landroid/net/Uri;)Landroid/net/Uri;
                        move-result-object v$authoritiesRegister
                    """
                )

                addInstructionsAtControlFlowLabel(
                    serviceActionIndex,
                    """
                        invoke-static { v$serviceActionRegister }, $EXTENSION_CLASS->overrideServiceAction(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$serviceActionRegister
                    """
                )
            }
        } else {
            LegacyServiceBindIntentUtilsFingerprint.let {
                it.method.apply {
                    val serviceActionMatch = it.instructionMatches[1]
                    val serviceActionIndex = serviceActionMatch.index
                    val serviceActionRegister = serviceActionMatch.instruction.registersUsed[2]

                    val authoritiesMatch = it.instructionMatches[2]
                    val authoritiesIndex = authoritiesMatch.index
                    val authoritiesRegister = authoritiesMatch.instruction.registersUsed[1]
                    val bundleRegister = authoritiesMatch.instruction.registersUsed[4]

                    addInstructionsAtControlFlowLabel(
                        authoritiesIndex,
                        """
                            invoke-static { v$authoritiesRegister, v$bundleRegister }, $EXTENSION_CLASS->overrideAuthorities(Landroid/net/Uri;Landroid/os/Bundle;)Landroid/net/Uri;
                            move-result-object v$authoritiesRegister
                        """
                    )

                    addInstructionsAtControlFlowLabel(
                        serviceActionIndex,
                        """
                            invoke-static { v$serviceActionRegister }, $EXTENSION_CLASS->overrideServiceAction(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v$serviceActionRegister
                        """
                    )
                }
            }
        }

        updatePatchStatus(EXTENSION_PATCH_STATUS_CLASS_DESCRIPTOR, "PoTokenProvider")

        executeBlock()
    }
}
