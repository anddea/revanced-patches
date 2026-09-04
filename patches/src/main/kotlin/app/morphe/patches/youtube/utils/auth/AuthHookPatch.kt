/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.utils.auth

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.extension.Constants.EXTENSION_PATH
import app.morphe.patches.shared.misc.request.buildRequestPatch
import app.morphe.patches.shared.misc.request.hookBuildRequest
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.playservice.is_21_02_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.util.findFieldFromToString
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/innertube/utils/AuthUtils;"

val authHookPatch = bytecodePatch(
    description = "Hook to get the parameters required for account authentication."
) {
    dependsOn(
        sharedExtensionPatch,
        buildRequestPatch,
        versionCheckPatch,
    )

    execute {
        val (pageIdField, incognitoField) =
            with(AccountIdentityToStringFingerprint.method) {
                Pair(
                    findFieldFromToString(GET_PAGE_ID_STRING),
                    findFieldFromToString(IS_INCOGNITO_STRING)
                )
            }

        val pageIdFingerprints = mutableListOf(getPageIdFingerprint(pageIdField))

        if (is_21_02_or_greater) {
            pageIdFingerprints += isEmptyPageIdFingerprint(pageIdField)
        }

        pageIdFingerprints.forEach {
            it.method.apply {
                val index = it.instructionMatches.first().index
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR->setPageId(Ljava/lang/String;)V"
                )
            }
        }

        getIncognitoStatusFingerprint(incognitoField).matchAll().forEach {
            it.method.apply {
                val index = it.instructionMatches.first().index
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstruction(
                    index + 1,
                    "invoke-static { v$register }, $EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR->setIncognitoStatus(Z)V"
                )
            }
        }

        hookBuildRequest(
            "$EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR->setRequestHeaders(Ljava/lang/String;Ljava/util/Map;)V"
        )
    }
}
