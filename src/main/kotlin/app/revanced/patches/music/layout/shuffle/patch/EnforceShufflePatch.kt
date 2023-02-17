package app.revanced.patches.music.layout.shuffle.patch

import app.revanced.extensions.toErrorResult
import app.revanced.extensions.transformFields
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.*
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.TypeUtil.traverseClassHierarchy
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.toInstructions
import app.revanced.patches.music.layout.shuffle.fingerprints.MusicPlaybackControlsFingerprint
import app.revanced.patches.music.layout.shuffle.fingerprints.ShuffleClassFingerprint
import app.revanced.patches.music.layout.shuffle.fingerprints.ShuffleClassReferenceFingerprint
import app.revanced.patches.music.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.patches.music.misc.settings.patch.MusicSettingsPatch
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.integrations.Constants.MUSIC_SETTINGS_PATH
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.immutable.ImmutableField
import org.jf.dexlib2.immutable.ImmutableMethod
import org.jf.dexlib2.immutable.ImmutableMethodImplementation
import org.jf.dexlib2.immutable.ImmutableMethodParameter

@Patch
@Name("enable-force-shuffle")
@Description("Enable force shuffle even if another track is played.")
@DependsOn(
    [
        MusicSettingsPatch::class,
        SharedResourcdIdPatch::class
    ]
)
@YouTubeMusicCompatibility
@Version("0.0.1")
class EnforceShufflePatch : BytecodePatch(
    listOf(
        MusicPlaybackControlsFingerprint,
        ShuffleClassFingerprint,
        ShuffleClassReferenceFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        ShuffleClassReferenceFingerprint.result?.let { result ->
            val startIndex = result.scanResult.patternScanResult!!.startIndex
            val endIndex = result.scanResult.patternScanResult!!.endIndex
            SHUFFLE_CLASS = result.classDef.type

            with (result.mutableMethod.implementation!!.instructions) {
                firstRef = (elementAt(startIndex) as ReferenceInstruction).reference as FieldReference
                secondRef = (elementAt(startIndex + 1) as ReferenceInstruction).reference as DexBackedMethodReference
                thirdRef = (elementAt(endIndex) as ReferenceInstruction).reference as FieldReference

                this.filter { instruction ->
                    val fieldReference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                    fieldReference?.let { it.type == "Landroid/widget/ImageView;" } == true
                }.forEach { instruction ->
                    fourthRef = (instruction as ReferenceInstruction).reference as FieldReference
                }
            }
        } ?: return ShuffleClassReferenceFingerprint.toErrorResult()


        ShuffleClassFingerprint.result?.let {
            it.mutableMethod.addInstruction(
                it.scanResult.patternScanResult!!.endIndex,
                "sput-object p0, $MUSIC_PLAYBACK_CONTROLS_CLASS_DESCRIPTOR->shuffleclass:$SHUFFLE_CLASS"
            )

            context.traverseClassHierarchy(it.mutableClass) {
                accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL
                transformFields {
                    ImmutableField(
                        definingClass,
                        name,
                        type,
                        AccessFlags.PUBLIC or AccessFlags.PUBLIC,
                        null,
                        null,
                        null
                    ).toMutable()
                }
            }
        } ?: return ShuffleClassFingerprint.toErrorResult()


        MusicPlaybackControlsFingerprint.result?.let {
            with (it.mutableMethod.implementation!!.instructions) {
                fifthRef = (elementAt(0) as ReferenceInstruction).reference as FieldReference
                sixthRef = (elementAt(1) as ReferenceInstruction).reference as DexBackedMethodReference
            }

            it.mutableClass.staticFields.add(
                ImmutableField(
                    it.mutableMethod.definingClass,
                    "shuffleclass",
                    SHUFFLE_CLASS,
                    AccessFlags.PUBLIC or AccessFlags.STATIC,
                    null,
                    null,
                    null
                ).toMutable()
            )

            it.mutableMethod.addInstructions(
                0,
                """
                invoke-virtual {v0, v1}, $MUSIC_PLAYBACK_CONTROLS_CLASS_DESCRIPTOR->buttonHook(Z)V
                return-void
            """
            )

            it.mutableClass.methods.add(
                ImmutableMethod(
                    it.classDef.type,
                    "buttonHook",
                    listOf(ImmutableMethodParameter("Z", null, null)),
                    "V",
                    AccessFlags.PUBLIC or AccessFlags.FINAL,
                    null,
                    null,
                    ImmutableMethodImplementation(
                        5, """
                            invoke-static {}, $MUSIC_SETTINGS_PATH->enableForceShuffle()Z
                            move-result v0
                            if-eqz v0, :cond_0
                            new-instance v0, $SHUFFLE_CLASS
                            sget-object v0, $MUSIC_PLAYBACK_CONTROLS_CLASS_DESCRIPTOR->shuffleclass:$SHUFFLE_CLASS
                            iget-object v1, v0, $SHUFFLE_CLASS->${firstRef.name}:${firstRef.type}
                            invoke-interface {v1}, $secondRef
                            move-result-object v1
                            check-cast v1, ${thirdRef.definingClass}
                            iget-object v1, v1, ${thirdRef.definingClass}->${thirdRef.name}:${thirdRef.type}
                            invoke-virtual {v1}, ${thirdRef.type}->ordinal()I
                            move-result v1
                            iget-object v2, v0, $SHUFFLE_CLASS->${fourthRef.name}:Landroid/widget/ImageView;
                            invoke-virtual {v2}, Landroid/widget/ImageView;->performClick()Z
                            if-eqz v1, :cond_0
                            invoke-virtual {v2}, Landroid/widget/ImageView;->performClick()Z
                            :cond_0
                            iput-boolean v4, v3, $MUSIC_PLAYBACK_CONTROLS_CLASS_DESCRIPTOR->${fifthRef.name}:${fifthRef.type}
                            invoke-virtual {v3}, $sixthRef
                            return-void
                        """.toInstructions(), null, null
                    )
                ).toMutable()
            )
        } ?: return MusicPlaybackControlsFingerprint.toErrorResult()

        MusicSettingsPatch.addMusicPreference("listening", "revanced_enable_force_shuffle", "true")

        return PatchResultSuccess()
    }

    companion object {
        const val MUSIC_PLAYBACK_CONTROLS_CLASS_DESCRIPTOR =
            "Lcom/google/android/apps/youtube/music/watchpage/MusicPlaybackControls;"

        private lateinit var SHUFFLE_CLASS: String

        private lateinit var firstRef: FieldReference
        private lateinit var secondRef: DexBackedMethodReference
        private lateinit var thirdRef: FieldReference
        private lateinit var fourthRef: FieldReference

        private lateinit var fifthRef: FieldReference
        private lateinit var sixthRef: DexBackedMethodReference
    }
}
