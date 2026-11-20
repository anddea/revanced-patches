/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.navigation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private const val PIVOT_BAR_CLASS = "Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;"

/**
 * Sets the bottom padding of the navigation bar to the bottom window inset, the room for
 * the system navigation buttons when the bar is drawn behind them. The bar height also grows
 * by the same value, which is first stored in a field of the bar and then added to its height.
 */
internal object NavigationBarBottomPaddingFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT,
            definingClass = PIVOT_BAR_CLASS,
            type = "I"
        ),
        // Sets the bar height to its content height and the room in the field.
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            definingClass = PIVOT_BAR_CLASS,
            parameters = listOf(),
            returnType = "V",
            location = MatchAfterImmediately()
        ),
        methodCall(
            definingClass = PIVOT_BAR_CLASS,
            name = "setPadding",
            location = MatchAfterWithin(10)
        )
    )
)

/**
 * Gives the blurred background of the navigation bar its tint color, read from the theme.
 * The last filter calls the method that makes the tint paint, [NavigationBarBlurTintFingerprint].
 */
internal object NavigationBarBlurTintCallFingerprint : Fingerprint(
    definingClass = PIVOT_BAR_CLASS,
    filters = listOf(
        methodCall(smali = "Landroid/content/res/TypedArray;->getColor(II)I"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf("I"),
            returnType = "V",
            location = MatchAfterImmediately()
        ),
        methodCall(
            smali = "$PIVOT_BAR_CLASS->setBackground(Landroid/graphics/drawable/Drawable;)V",
            location = MatchAfterWithin(3)
        )
    )
)

/**
 * Makes the tint paint of the blurred background of the navigation bar, drawn over the blur.
 * Resolved against the method called by [NavigationBarBlurTintCallFingerprint].
 */
internal object NavigationBarBlurTintFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        methodCall(smali = "Landroid/graphics/Paint;->setColor(I)V"),
        methodCall(smali = "Landroid/graphics/Paint;->setAlpha(I)V", location = MatchAfterWithin(4))
    )
)


internal object BottomUiContainerOnLayoutFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/apps/youtube/app/common/ui/bottomui/BottomUiContainer;",
    name = "onLayout",
    filters = listOf(
        methodCall(smali = "Landroid/widget/FrameLayout;->onLayout(ZIIII)V")
    )
)

