/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.utils.navigation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.findFreeRegister
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import java.lang.ref.WeakReference

private lateinit var browseIdMethodRef: WeakReference<MutableMethod>
private var browseIdInsertIndex = -1
private var browseIdRegister = -1

private lateinit var searchResultsMethodRef: WeakReference<MutableMethod>

/**
 * Calls the extension method with the browse id of every browse page that is shown, including
 * a page that is returned to and served from cache. Channel pages use the channel id as browse id.
 *
 * @param descriptor Static extension method with a single String parameter, which can be null.
 */
fun addBrowseIdHook(descriptor: String) {
    browseIdMethodRef.get()!!.addInstruction(
        browseIdInsertIndex,
        "invoke-static { v$browseIdRegister }, $descriptor"
    )
}

/**
 * Calls the extension method when the search feed is shown. It is not a browse page,
 * so returning to it from a browse page does not call the hooks of [addBrowseIdHook].
 *
 * @param descriptor Static extension method with no parameters.
 */
fun addSearchResultsHook(descriptor: String) {
    searchResultsMethodRef.get()!!.addInstruction(
        0,
        "invoke-static { }, $descriptor"
    )
}

internal val browseIdHookPatch = bytecodePatch(description = "Hooks the browse id of the page that is shown.") {
    dependsOn(sharedExtensionPatch)

    execute {
        BrowseFragmentOnCreateViewFingerprint.let {
            it.method.apply {
                // The endpoint of the page is read just before it is checked for browse data.
                val endpointIndex = it.instructionMatches[1].index
                val endpointField = getInstruction<ReferenceInstruction>(endpointIndex)
                    .getReference<FieldReference>()!!

                // Browse data is an extension of the endpoint, and reading it starts with that field.
                val browseDataExtensionIndex = indexOfFirstInstructionOrThrow(
                    endpointIndex, Opcode.SGET_OBJECT
                )
                val browseDataExtensionField = getInstruction<ReferenceInstruction>(browseDataExtensionIndex)
                    .getReference<FieldReference>()!!

                // Matches two 100% identical methods in the same class, either method works here.
                val browseIdMethod = Fingerprint(
                    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
                    returnType = "Ljava/lang/String;",
                    parameters = listOf(endpointField.type),
                    filters = listOf(
                        fieldAccess(opcode = Opcode.SGET_OBJECT, reference = browseDataExtensionField)
                    )
                ).matchAll(1..2).first().method

                // The fragment is handed the endpoint of every page it shows, including a page that
                // is returned to and served from cache, which makes no browse request to read.
                Fingerprint(
                    definingClass = definingClass,
                    returnType = "V",
                    parameters = listOf(endpointField.type),
                    filters = listOf(
                        fieldAccess(opcode = Opcode.IPUT_OBJECT, reference = endpointField)
                    )
                ).method.apply {
                    val insertIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.IPUT_OBJECT && getReference<FieldReference>() == endpointField
                    } + 1
                    val freeRegister = findFreeRegister(insertIndex)

                    addInstructions(
                        insertIndex,
                        """
                            invoke-static { p1 }, $browseIdMethod
                            move-result-object v$freeRegister
                        """
                    )

                    browseIdMethodRef = WeakReference(this)
                    browseIdInsertIndex = insertIndex + 2
                    browseIdRegister = freeRegister
                }
            }
        }

        searchResultsMethodRef = WeakReference(SearchResultsFragmentOnCreateViewFingerprint.method)
    }
}

