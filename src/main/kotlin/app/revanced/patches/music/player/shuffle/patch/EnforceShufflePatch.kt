package app.revanced.patches.music.player.shuffle.patch

import app.revanced.extensions.exception
import app.revanced.extensions.transformFields
import app.revanced.extensions.traverseClassHierarchy
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.music.player.shuffle.fingerprints.MusicPlaybackControlsFingerprint
import app.revanced.patches.music.player.shuffle.fingerprints.ShuffleClassFingerprint
import app.revanced.patches.music.player.shuffle.fingerprints.ShuffleClassReferenceFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_MISC_PATH
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

@Patch
@Name("Enable force shuffle")
@Description("Enable force shuffle even if another track is played.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@MusicCompatibility
class EnforceShufflePatch : BytecodePatch(
    listOf(
        MusicPlaybackControlsFingerprint,
        ShuffleClassFingerprint,
        ShuffleClassReferenceFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        ShuffleClassReferenceFingerprint.result?.let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val endIndex = it.scanResult.patternScanResult!!.endIndex
                val imageViewIndex = implementation!!.instructions.indexOfFirst { instruction ->
                    ((instruction as? ReferenceInstruction)?.reference as? FieldReference)?.type == "Landroid/widget/ImageView;"
                }

                SHUFFLE_CLASS = it.classDef.type

                shuffleReference1 = getInstruction(startIndex).descriptor
                shuffleReference2 = getInstruction(startIndex + 1).descriptor
                shuffleReference3 = getInstruction(endIndex).descriptor
                shuffleReference4 = getInstruction(imageViewIndex).descriptor
            }
        } ?: throw ShuffleClassReferenceFingerprint.exception


        ShuffleClassFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstruction(
                    it.scanResult.patternScanResult!!.endIndex,
                    "sput-object p0, $MUSIC_PLAYBACK_CONTROLS_CLASS_DESCRIPTOR->shuffleclass:$SHUFFLE_CLASS"
                )
            }

            context.traverseClassHierarchy(it.mutableClass) {
                accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL
                transformFields {
                    ImmutableField(
                        definingClass,
                        name,
                        type,
                        AccessFlags.PUBLIC or AccessFlags.PUBLIC,
                        null,
                        annotations,
                        null
                    ).toMutable()
                }
            }
        } ?: throw ShuffleClassFingerprint.exception

        MusicPlaybackControlsFingerprint.result?.let {
            it.mutableMethod.apply {
                shuffleReference5 = getInstruction(0).descriptor
                shuffleReference6 = getInstruction(1).descriptor

                addInstructions(
                    0, """
                        invoke-virtual {v0, v1}, $MUSIC_PLAYBACK_CONTROLS_CLASS_DESCRIPTOR->buttonHook(Z)V
                        return-void
                        """
                )
            }

            it.mutableClass.apply {
                staticFields.add(
                    ImmutableField(
                        it.mutableMethod.definingClass,
                        "shuffleclass",
                        SHUFFLE_CLASS,
                        AccessFlags.PUBLIC or AccessFlags.STATIC,
                        null,
                        annotations,
                        null
                    ).toMutable()
                )

                val shuffleFieldReference = shuffleReference3 as FieldReference

                methods.add(
                    ImmutableMethod(
                        it.classDef.type,
                        "buttonHook",
                        listOf(ImmutableMethodParameter("Z", annotations, null)),
                        "V",
                        AccessFlags.PUBLIC or AccessFlags.FINAL,
                        annotations,
                        null,
                        ImmutableMethodImplementation(
                            5, """
                            invoke-static {}, $MUSIC_MISC_PATH/ForceShufflePatch;->enableForceShuffle()Z
                            move-result v0
                            if-eqz v0, :cond_0
                            new-instance v0, $SHUFFLE_CLASS
                            sget-object v0, $MUSIC_PLAYBACK_CONTROLS_CLASS_DESCRIPTOR->shuffleclass:$SHUFFLE_CLASS
                            iget-object v1, v0, $shuffleReference1
                            invoke-interface {v1}, $shuffleReference2
                            move-result-object v1
                            check-cast v1, ${shuffleFieldReference.definingClass}
                            iget-object v1, v1, $shuffleReference3
                            invoke-virtual {v1}, ${shuffleFieldReference.type}->ordinal()I
                            move-result v1
                            iget-object v2, v0, $shuffleReference4
                            invoke-virtual {v2}, Landroid/widget/ImageView;->performClick()Z
                            if-eqz v1, :cond_0
                            invoke-virtual {v2}, Landroid/widget/ImageView;->performClick()Z
                            :cond_0
                            iput-boolean v4, v3, $shuffleReference5
                            invoke-virtual {v3}, $shuffleReference6
                            return-void
                            """.toInstructions(), null, null
                        )
                    ).toMutable()
                )
            }
        } ?: throw MusicPlaybackControlsFingerprint.exception

        SettingsPatch.addMusicPreference(
            CategoryType.PLAYER,
            "revanced_enable_force_shuffle",
            "true"
        )

    }

    private companion object {
        const val MUSIC_PLAYBACK_CONTROLS_CLASS_DESCRIPTOR =
            "Lcom/google/android/apps/youtube/music/watchpage/MusicPlaybackControls;"

        lateinit var SHUFFLE_CLASS: String
        lateinit var shuffleReference1: Reference
        lateinit var shuffleReference2: Reference
        lateinit var shuffleReference3: Reference
        lateinit var shuffleReference4: Reference
        lateinit var shuffleReference5: Reference
        lateinit var shuffleReference6: Reference

        val Instruction.descriptor
            get() = (this as ReferenceInstruction).reference
    }
}
