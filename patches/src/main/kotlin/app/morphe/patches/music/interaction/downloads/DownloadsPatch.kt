/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/1881
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.music.interaction.downloads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.music.actionbar.components.actionBarComponentsPatch
import app.morphe.patches.music.flyoutmenu.components.flyoutMenuComponentsPatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.patch.PatchList.DOWNLOADS
import app.morphe.patches.music.utils.playservice.is_7_25_or_greater
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.addPreferenceCategory
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.misc.fix.proto.fixProtoLibraryPatch
import app.morphe.patches.shared.misc.fix.proto.parseByteArrayMethodRef
import app.morphe.patches.shared.misc.media.MediaSessionSetMetadataFingerprint
import app.morphe.patches.shared.misc.media.hookMediaSessionArgument
import app.morphe.patches.shared.misc.request.authHeadersPatch
import app.morphe.util.Utils.printWarn
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val DOWNLOADS_PATH = "Lapp/morphe/extension/music/patches/downloads"
private const val EXTENSION_CLASS = "$DOWNLOADS_PATH/LocalDownloadManager;"
private const val NATIVE_DOWNLOADS = "$DOWNLOADS_PATH/NativeDownloads;"
private const val NATIVE_COMMAND = $$"$$DOWNLOADS_PATH/NativeDownloads$Command;"
private const val PLAYLIST_STORE = "$DOWNLOADS_PATH/NativePlaylistStore;"
private const val PLAYLIST_DATABASE = $$"$$DOWNLOADS_PATH/NativePlaylistStore$Database;"
private const val FUTURE = "Ljava/util/concurrent/Future;"
private const val NATIVE_FUTURE = "Lcom/google/common/util/concurrent/ListenableFuture;"
private const val REGISTRY = "Lcom/google/protobuf/ExtensionRegistryLite;"

private val downloadsResourcePatch = resourcePatch {
    dependsOn(settingsPatch, actionBarComponentsPatch)
    execute {
        addPreferenceCategory(CategoryType.MISC.value)
        addSwitchPreference(CategoryType.MISC, "morphe_music_in_app_downloads", "false", "revanced_external_downloader_action")
    }
}

private fun MutableClass.addDownloadsBridge(
    name: String, parameters: List<String>, returnType: String, registers: Int, instructions: String,
) {
    methods.add(
        ImmutableMethod(type, name, parameters.map { ImmutableMethodParameter(it, null, null) },
            returnType, AccessFlags.PUBLIC.value, null, null, MutableMethodImplementation(registers)
        ).toMutable().apply { addInstructions(0, instructions) }
    )
}

private fun Method.hasString(value: String): Boolean =
    implementation?.instructions?.any { it.getReference<StringReference>()?.string == value } == true

private fun List<Instruction>.hasZeroDiscriminator(callIndex: Int): Boolean {
    val register = when (val call = this[callIndex]) {
        is FiveRegisterInstruction -> if (call.registerCount == 4) call.registerF else return false
        is RegisterRangeInstruction -> if (call.registerCount == 4) call.startRegister + 3 else return false
        else -> return false
    }
    for (index in callIndex - 1 downTo 0) {
        val producer = this[index] as? OneRegisterInstruction ?: continue
        if (producer.registerA == register) {
            return producer is NarrowLiteralInstruction && producer.narrowLiteral == 0
        }
    }
    return false
}

@Suppress("unused")
val downloadsPatch = bytecodePatch(
    DOWNLOADS.title,
    DOWNLOADS.summary
) {
    dependsOn(
        actionBarComponentsPatch,
        authHeadersPatch,
        downloadsResourcePatch,
        fixProtoLibraryPatch,
        flyoutMenuComponentsPatch,
        settingsPatch,
        sharedExtensionPatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    execute {
        if (!is_7_25_or_greater) {
            printWarn("\"${DOWNLOADS.title}\" is not supported in this version. Use YouTube Music v7.25 or later.")
            return@execute
        }

        MediaSessionSetMetadataFingerprint.hookMediaSessionArgument(
            "$EXTENSION_CLASS->onSetMetadata(Landroid/media/MediaMetadata;)V"
        )

        // reparse with the generated registry so browse endpoint extensions survive.
        val resolver = nativeDownloadsCommandResolverFingerprint.method
        val command = mutableClassDefBy(resolver.parameters[1].type)
        command.interfaces.add(NATIVE_COMMAND)
        val defaultCommand = command.fields.single { it.type == command.type && AccessFlags.STATIC.isSet(it.accessFlags) }
        val parserOwner = parseByteArrayMethodRef.get()!!.definingClass
        val parser = mutableClassDefBy(parserOwner).methods.single {
            it.name == "parseFrom" && it.parameterTypes == listOf(parserOwner, "[B", REGISTRY)
        }

        command.addDownloadsBridge("rvxWithBytes", listOf("[B"), "Ljava/lang/Object;", 4, """
            sget-object v0, $defaultCommand
            invoke-static {}, $REGISTRY->getGeneratedRegistry()$REGISTRY
            move-result-object v1
            invoke-static {v0, p1, v1}, $parser
            move-result-object v0
            return-object v0
        """)

        // hook the dispatcher that calls tryResolve, before it picks a resolver.
        // Signed-in resolvers are bound to the command they were selected for, so rewriting inside tryResolve is too late.
        // The context map is passed because signed-in header Download buttons carry the playlist only in its endpoint tag.
        fun List<CharSequence>.names() = map { it.toString() }
        val resolverParameters = resolver.parameterTypes.names()
        val dispatcher = nativeDownloadsCommandResolverFingerprint.classDef.methods.single { method ->
            method.returnType == "V" && method.parameterTypes.names() == listOf(command.type, "Ljava/util/Map;") &&
                method.implementation?.instructions?.any { instruction ->
                    instruction.getReference<MethodReference>()?.let {
                        it.definingClass == resolver.definingClass && it.name == resolver.name &&
                            it.parameterTypes.names() == resolverParameters
                    } == true
                } == true
        }

        dispatcher.addInstructionsWithLabels(0, """
            invoke-static/range {p1 .. p2}, $NATIVE_DOWNLOADS->rewriteCommand(Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;
            move-result-object v0
            if-nez v0, :native_music_navigation
            return-void
            :native_music_navigation
            check-cast v0, ${command.type}
            move-object p1, v0
        """)

        // Wrap the existing async repository instead of opening Music's SQLite file independently.
        val constructor = nativePlaylistDatabaseFingerprint.method
        val repository = nativePlaylistDatabaseFingerprint.classDef
        val factoryCall = constructor.implementation!!.instructions.mapNotNull { it.getReference<MethodReference>() }
            .single { it.parameterTypes.firstOrNull() == "Ljava/lang/String;" && it.parameterTypes.size == 2 }
        fun Method.usesPlaylistDatabase() = implementation!!.instructions
            .mapNotNull { it.getReference<MethodReference>() }
            .any { it.definingClass == factoryCall.returnType && it.returnType == NATIVE_FUTURE }
        val create = repository.methods.single {
            it.parameterTypes == listOf("Ljava/lang/String;") && it.returnType == NATIVE_FUTURE
                    && it.usesPlaylistDatabase()
        }
        val append = repository.methods.single {
            it.parameterTypes.size == 2 && it.parameterTypes[0] == "J"
                    && it.parameterTypes[1].startsWith("L") && it.parameterTypes[1] != "Ljava/lang/String;"
                    && it.returnType == NATIVE_FUTURE && it.usesPlaylistDatabase()
        }
        val immutableList = append.parameters[1].type
        val copy = mutableClassDefBy(immutableList).methods.single {
            AccessFlags.STATIC.isSet(it.accessFlags) && it.parameterTypes == listOf("Ljava/util/Collection;")
                    && it.returnType == immutableList
        }
        // Newer R8 merges delete/query lambdas. Discriminator zero is the delete transaction;
        // older unmerged lambdas have just a (long) constructor. Validate the SQL selection too.
        val delete = repository.methods.filter {
            it.parameterTypes == listOf("J") && it.returnType == NATIVE_FUTURE && it.usesPlaylistDatabase()
        }.single { method ->
            val instructions = method.implementation!!.instructions.toList()
            instructions.indices.any { index ->
                val reference = instructions[index].getReference<MethodReference>() ?: return@any false
                if (reference.name != "<init>" || reference.parameterTypes.firstOrNull() != "J") return@any false
                val transaction = mutableClassDefBy(reference.definingClass)
                transaction.methods.any { it.hasString("music_sideloaded_playlist") && it.hasString("id = ?") } &&
                    (reference.parameterTypes == listOf("J") ||
                        (reference.parameterTypes == listOf("J", "I") && instructions.hasZeroDiscriminator(index)))
            }
        }
        repository.interfaces.add(PLAYLIST_DATABASE)
        repository.addDownloadsBridge("rvxCreatePlaylist", listOf("Ljava/lang/String;"), FUTURE, 3, """
            invoke-virtual {p0, p1}, $create
            move-result-object v0
            return-object v0
        """)
        repository.addDownloadsBridge("rvxAddPlaylistTracks", listOf("J", "Ljava/util/List;"), FUTURE, 6, """
            invoke-static {p3}, $copy
            move-result-object v0
            invoke-virtual {p0, p1, p2, v0}, $append
            move-result-object v0
            return-object v0
        """)
        repository.addDownloadsBridge("rvxDeletePlaylist", listOf("J"), FUTURE, 4, """
            invoke-virtual {p0, p1, p2}, $delete
            move-result-object v0
            return-object v0
        """)
        // Music allows one open helper per database.
        // Reuse the app's own repository instance; constructing another one is rejected.
        val ctorInstructions = constructor.implementation!!.instructions.toList()
        val returnIndex = ctorInstructions.indexOfLast { it.opcode == Opcode.RETURN_VOID }
        if (returnIndex < 0) throw PatchException("Native local-playlist repository constructor has no return")
        constructor.addInstruction(returnIndex,
            "invoke-static/range {p0 .. p0}, $PLAYLIST_STORE->onDatabase(Ljava/lang/Object;)V")

        updatePatchStatus(DOWNLOADS)
    }
}
