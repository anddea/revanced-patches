/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.layout.music

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.patch.PatchList.OVERRIDE_YOUTUBE_MUSIC_BUTTONS
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.findMutableMethodOf
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS = "Lapp/morphe/extension/youtube/patches/OverrideYouTubeMusicButtonsPatch;"

private fun overrideYouTubeMusicManifestPatch() = resourcePatch {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        var manifestContent = manifestFile.readText()
        val permissionTag = "<uses-permission android:name=\"android.permission.QUERY_ALL_PACKAGES\"/>"

        if (!manifestContent.contains(permissionTag)) {
            manifestContent = manifestContent.replace(
                "<application",
                "$permissionTag\n    <application"
            )
            manifestFile.writeText(manifestContent)
        }
    }
}

@Suppress("unused")
val overrideYouTubeMusicButtonsPatch = bytecodePatch(
    OVERRIDE_YOUTUBE_MUSIC_BUTTONS.title,
    OVERRIDE_YOUTUBE_MUSIC_BUTTONS.summary,
) {
    dependsOn(settingsPatch)
    dependsOn(overrideYouTubeMusicManifestPatch())
    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: HOOK_BUTTONS",
                "SETTINGS: OVERRIDE_YOUTUBE_MUSIC_BUTTONS"
            ),
            OVERRIDE_YOUTUBE_MUSIC_BUTTONS
        )

        classDefForEach { classDef ->
            if (classDef.type == EXTENSION_CLASS) return@classDefForEach
            var needsPatch = false
            classDef.methods.forEach { method ->
                if (method.implementation?.instructions?.any {
                        it.opcode == Opcode.INVOKE_VIRTUAL &&
                                (it as? ReferenceInstruction)?.reference?.let { ref ->
                                    val mRef = ref as? MethodReference
                                    mRef?.definingClass == "Landroid/content/Intent;" &&
                                            (mRef.name == "setPackage" || mRef.name == "setData" || mRef.name == "setComponent")
                                } == true
                    } == true) {
                    needsPatch = true
                }
            }

            if (needsPatch) {
                val mutableClass = mutableClassDefBy(classDef.type)
                classDef.methods.forEach { method ->
                    val instructions = method.implementation?.instructions?.toList() ?: return@forEach
                    val targetIndices = instructions.mapIndexedNotNull { index, instruction ->
                        if (instruction.opcode == Opcode.INVOKE_VIRTUAL) {
                            val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                            if (ref?.definingClass == "Landroid/content/Intent;") {
                                if (ref.name == "setPackage" && ref.parameterTypes == listOf("Ljava/lang/String;")) {
                                    index to "overrideSetPackage(Landroid/content/Intent;Ljava/lang/String;)Landroid/content/Intent;"
                                } else if (ref.name == "setData" && ref.parameterTypes == listOf("Landroid/net/Uri;")) {
                                    index to "overrideSetData(Landroid/content/Intent;Landroid/net/Uri;)Landroid/content/Intent;"
                                } else if (ref.name == "setComponent" && ref.parameterTypes == listOf("Landroid/content/ComponentName;")) {
                                    index to "overrideSetComponent(Landroid/content/Intent;Landroid/content/ComponentName;)Landroid/content/Intent;"
                                } else null
                            } else null
                        } else null
                    }

                    if (targetIndices.isNotEmpty()) {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        targetIndices.reversed().forEach { (index, methodDescriptor) ->
                            val instruction = instructions[index]
                            val invokeString = if (instruction is RegisterRangeInstruction) {
                                "invoke-static/range {v${instruction.startRegister} .. v${instruction.startRegister + instruction.registerCount - 1}}"
                            } else {
                                val i = instruction as FiveRegisterInstruction
                                "invoke-static {v${i.registerC}, v${i.registerD}}"
                            }

                            mutableMethod.replaceInstruction(
                                index,
                                "$invokeString, $EXTENSION_CLASS->$methodDescriptor"
                            )
                        }
                    }
                }
            }
        }
    }
}
