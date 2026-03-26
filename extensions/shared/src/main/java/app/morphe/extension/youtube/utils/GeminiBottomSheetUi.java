/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Attribution Notice
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Attribution (Section 7(b)): This specific copyright notice and the
 *    list of original authors above must be preserved in any copy or
 *    derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin (Section 7(c)): Modified versions must be clearly marked as
 *    such (e.g., by adding a "Modified by" line or a new copyright notice).
 *    They must not be misrepresented as the original work.
 *
 * ------------------------------------------------------------------------
 * Version Control Acknowledgement (Non-binding Request)
 * ------------------------------------------------------------------------
 *
 * While not a legal requirement of the GPLv3, the original author(s)
 * respectfully request that ports or substantial modifications retain
 * historical authorship credit in version control systems (e.g., Git),
 * listing original author(s) appropriately and modifiers as committers
 * or co-authors.
 */

package app.morphe.extension.youtube.utils;

import static app.morphe.extension.shared.utils.ResourceUtils.getDrawable;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.dipToPixels;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import app.morphe.extension.shared.ui.SheetBottomDialog;
import app.morphe.extension.shared.utils.Utils;

final class GeminiBottomSheetUi {
    private static final int SECONDARY_BUTTON_BACKGROUND_COLOR = Color.parseColor("#33888888");
    private static final float SHEET_HEIGHT_FRACTION = 0.9f;
    private static final int MIN_SHEET_HEIGHT_DP = 240;
    private GeminiBottomSheetUi() {
    }

    interface OnSendListener {
        void onSendRequested(@NonNull SummarySheet sheet, @NonNull String question);
    }

    interface OnCopyListener {
        void onCopyRequested(@NonNull String text);
    }

    interface MessageFormatter {
        @NonNull
        CharSequence format(@NonNull String text);
    }

    interface OnConversationChangedListener {
        void onConversationChanged(@NonNull List<GeminiUtils.ChatMessage> conversationHistory);
    }

    static final class LoadingSheet {
        private final SheetBottomDialog.SlideDialog dialog;
        private final TextView metaView;
        private final ShimmerTextView statusView;
        private final TextView elapsedView;
        private final View previewContainer;
        private final TextView previewView;
        private final MaxHeightScrollView previewScrollView;
        private boolean dismissedByOwner;
        private boolean followPreviewBottom;

        LoadingSheet(
                @NonNull Context context,
                @NonNull String title,
                @Nullable String metaText,
                @NonNull String statusText,
                @NonNull Runnable onCancel,
                @NonNull Runnable onUserDismiss
        ) {
            final int dip8 = dipToPixels(8);
            final int dip12 = dipToPixels(12);
            final int dip16 = dipToPixels(16);
            final int dip20 = dipToPixels(20);

            SheetBottomDialog.DraggableLinearLayout mainLayout =
                    SheetBottomDialog.createMainLayout(context, ThemeUtils.getDialogBackgroundColor());
            mainLayout.setPadding(dip20, 0, dip20, dip20);

            TextView titleView = createTitleView(context, title);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            titleParams.topMargin = dip20;
            mainLayout.addView(titleView, titleParams);

            metaView = createMetaView(context);
            mainLayout.addView(metaView);
            setTextOrHide(metaView, metaText);

            statusView = new ShimmerTextView(context);
            statusView.setText(statusText);
            statusView.setTextColor(ThemeUtils.getAppForegroundColor());
            statusView.setTextSize(16);
            statusView.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            statusParams.topMargin = dip12;
            mainLayout.addView(statusView, statusParams);

            elapsedView = new TextView(context);
            elapsedView.setTextColor(ThemeUtils.getAppForegroundColor());
            elapsedView.setTextSize(13);
            elapsedView.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams elapsedParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            elapsedParams.topMargin = dip8;
            mainLayout.addView(elapsedView, elapsedParams);

            previewScrollView = new MaxHeightScrollView(context);
            previewScrollView.setMaxHeight((int) (context.getResources().getDisplayMetrics().heightPixels * 0.34f));
            previewScrollView.setVerticalScrollBarEnabled(false);
            previewScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            previewScrollView.setFillViewport(true);
            previewScrollView.setOnScrollPositionChangedListener(
                    () -> followPreviewBottom = previewScrollView.isNearBottom(dipToPixels(28))
            );
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            previewParams.topMargin = dip16;

            previewView = new TextView(context);
            previewView.setTextColor(ThemeUtils.getAppForegroundColor());
            previewView.setTextSize(15);
            previewView.setTextIsSelectable(false);
            previewView.setPadding(dip16, dip16, dip16, dip16);
            previewView.setBackground(createRoundedBackground(
                    ThemeUtils.getAdjustedBackgroundColor(false),
                    18
            ));
            previewScrollView.addView(previewView, new ScrollView.LayoutParams(
                    ScrollView.LayoutParams.MATCH_PARENT,
                    ScrollView.LayoutParams.WRAP_CONTENT
            ));
            previewScrollView.setVisibility(View.GONE);
            mainLayout.addView(previewScrollView, previewParams);
            previewContainer = previewScrollView;
            followPreviewBottom = false;

            LinearLayout buttonsRow = createButtonsRow(context);
            Button cancelButton = createActionButton(context, str("revanced_cancel"), true);
            cancelButton.setOnClickListener(v -> {
                dismissedByOwner = true;
                onCancel.run();
            });
            addWeightedButton(buttonsRow, cancelButton);
            mainLayout.addView(buttonsRow);

            dialog = SheetBottomDialog.createSlideDialog(context, mainLayout, 180);
            dialog.setOnDismissListener(d -> {
                statusView.stopShimmer();
                if (!dismissedByOwner) {
                    onUserDismiss.run();
                }
            });

            Window window = dialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            }
            statusView.startShimmer();
            dialog.show();
        }

        boolean isShowing() {
            return dialog.isShowing();
        }

        void dismiss() {
            dismissedByOwner = true;
            statusView.stopShimmer();
            dialog.dismiss();
        }

        void updateMeta(@Nullable String metaText) {
            setTextOrHide(metaView, metaText);
        }

        void updateStatus(@NonNull String statusText) {
            statusView.setText(statusText);
        }

        void updateElapsed(int seconds) {
            elapsedView.setText(seconds >= 0 ? seconds + "s" : "");
        }

        void updatePreview(@NonNull String previewText) {
            previewContainer.setVisibility(View.VISIBLE);
            previewView.setVisibility(View.VISIBLE);
            previewView.setText(previewText);

            if (followPreviewBottom) {
                previewScrollView.post(() -> previewScrollView.fullScroll(View.FOCUS_DOWN));
            }
        }
    }

    static final class SummarySheet {
        private final Context context;
        private final SheetBottomDialog.SlideDialog dialog;
        private final LinearLayout headerContainer;
        private final View headerSpacerView;
        private final Drawable headerOverlayBackground;
        private final LinearLayout messagesContainer;
        private final MaxHeightScrollView messagesScrollView;
        private final LinearLayout footerContainer;
        private final FrostedSurface inputSurface;
        private final FrostedSurface sendSurface;
        private final EditText inputView;
        private final ImageButton sendButton;
        private final TextView persistentHeaderView;
        private final MessageFormatter assistantMessageFormatter;
        private final OnCopyListener onCopyListener;
        private final OnConversationChangedListener onConversationChangedListener;
        @Nullable
        private final CollapsibleHeaderSection summaryTitleSection;
        @Nullable
        private final CollapsibleHeaderSection summaryInfoSection;
        private final List<GeminiUtils.ChatMessage> conversationHistory = new ArrayList<>();
        private final int contentHorizontalPadding;
        private final int footerVerticalPadding;
        private final int headerExpandedTopMargin;
        private final int headerCollapsedTopMargin;
        private final int persistentHeaderExpandedTopMargin;
        private final int persistentHeaderCollapsedTopMargin;
        private final int messagesExpandedTopMargin;
        private final int messagesCollapsedTopMargin;
        private final int headerCollapseTranslation;
        private final int headerOverlayFadeDistance;
        private final int headerOverlayMaxAlpha;
        private final boolean collapsePersistentHeaderToSingleLine;
        @Nullable
        private String pendingUserQuestion;
        @Nullable
        private MessageEntry pendingAssistantEntry;
        @Nullable
        private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;
        @Nullable
        private View keyboardObserverView;
        private boolean dismissedByOwner;
        private boolean followConversationBottom;
        private float headerCollapseProgress = -1f;
        private int lastKeyboardInset;

        SummarySheet(
                @NonNull Context context,
                @Nullable String metaText,
                @Nullable String infoText,
                @NonNull CharSequence summaryText,
                @NonNull String rawSummaryText,
                @NonNull List<GeminiUtils.ChatMessage> initialConversationHistory,
                @NonNull MessageFormatter assistantMessageFormatter,
                @NonNull OnCopyListener onCopyListener,
                @NonNull OnConversationChangedListener onConversationChangedListener,
                @NonNull Runnable onUserDismiss,
                @NonNull OnSendListener onSendListener
        ) {
            this.context = context;
            this.assistantMessageFormatter = assistantMessageFormatter;
            this.onCopyListener = onCopyListener;
            this.onConversationChangedListener = onConversationChangedListener;

            final int dip4 = dipToPixels(4);
            final int dip8 = dipToPixels(8);
            final int dip10 = dipToPixels(10);
            final int dip12 = dipToPixels(12);
            final int dip16 = dipToPixels(16);
            final int dip20 = dipToPixels(20);
            final int dip42 = dipToPixels(42);

            SheetBottomDialog.DraggableLinearLayout mainLayout =
                    SheetBottomDialog.createMainLayout(context, ThemeUtils.getDialogBackgroundColor());
            contentHorizontalPadding = dip16;
            footerVerticalPadding = dip8;
            headerExpandedTopMargin = dip20;
            headerCollapsedTopMargin = dip8;
            messagesExpandedTopMargin = dip16;
            messagesCollapsedTopMargin = dip8;
            persistentHeaderCollapsedTopMargin = 0;
            headerCollapseTranslation = dip12;
            headerOverlayFadeDistance = dipToPixels(24);
            headerOverlayMaxAlpha = ThemeUtils.isDarkModeEnabled() ? 208 : 236;
            mainLayout.setPadding(contentHorizontalPadding, 0, contentHorizontalPadding, 0);

            headerContainer = new LinearLayout(context);
            headerContainer.setOrientation(LinearLayout.VERTICAL);
            headerContainer.setPadding(0, 0, 0, messagesExpandedTopMargin);
            headerOverlayBackground = createHeaderOverlayBackground(ThemeUtils.getDialogBackgroundColor());
            headerOverlayBackground.setAlpha(0);
            headerContainer.setBackground(headerOverlayBackground);

            TextView titleView = createTitleView(context, str("revanced_gemini_summary_title"));
            TextView metaView = createMetaView(context);
            setTextOrHide(metaView, metaText);
            if (metaView.getVisibility() == View.VISIBLE) {
                summaryTitleSection = CollapsibleHeaderSection.create(context, titleView, 0);
                headerContainer.addView(summaryTitleSection.container);
                headerContainer.addView(metaView);
                persistentHeaderView = metaView;
                persistentHeaderExpandedTopMargin = dip8;
                collapsePersistentHeaderToSingleLine = true;
            } else {
                summaryTitleSection = null;
                headerContainer.addView(titleView);
                headerContainer.addView(metaView);
                persistentHeaderView = titleView;
                persistentHeaderExpandedTopMargin = 0;
                collapsePersistentHeaderToSingleLine = false;
            }

            TextView infoView = createSecondaryTextView(context);
            setTextOrHide(infoView, infoText);
            if (infoView.getVisibility() == View.VISIBLE) {
                summaryInfoSection = CollapsibleHeaderSection.create(context, infoView, dip4);
                headerContainer.addView(summaryInfoSection.container);
            } else {
                summaryInfoSection = null;
            }

            messagesScrollView = new MaxHeightScrollView(context);
            messagesScrollView.setVerticalScrollBarEnabled(false);
            messagesScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            messagesScrollView.setFillViewport(true);
            messagesScrollView.setClipToPadding(false);
            messagesScrollView.setOnScrollPositionChangedListener(() -> {
                followConversationBottom = messagesScrollView.isNearBottom(dipToPixels(32));
                updateCollapsingHeader();
                updateHeaderOverlay();
                refreshFrostedSurfaces();
            });
            LinearLayout.LayoutParams messagesParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
            );

            messagesContainer = new LinearLayout(context);
            messagesContainer.setOrientation(LinearLayout.VERTICAL);
            headerSpacerView = new View(context);
            messagesContainer.addView(headerSpacerView, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0
            ));
            messagesScrollView.addView(messagesContainer, new ScrollView.LayoutParams(
                    ScrollView.LayoutParams.MATCH_PARENT,
                    ScrollView.LayoutParams.WRAP_CONTENT
            ));

            FrameLayout conversationFrame = new FrameLayout(context);
            conversationFrame.setClipChildren(false);
            conversationFrame.setClipToPadding(false);
            mainLayout.addView(conversationFrame, messagesParams);
            conversationFrame.addView(messagesScrollView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP
            );
            headerParams.topMargin = headerExpandedTopMargin;
            conversationFrame.addView(headerContainer, headerParams);
            headerContainer.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                updateMessageViewportInset();
                updateHeaderOverlay();
                refreshFrostedSurfaces();
            });
            followConversationBottom = false;

            appendSummaryMessage(summaryText, rawSummaryText);

            for (GeminiUtils.ChatMessage message : initialConversationHistory) {
                conversationHistory.add(message);
                if ("user".equals(message.role())) {
                    appendUserMessage(message.text(), false);
                } else {
                    appendAssistantMessage(message.text());
                }
            }

            footerContainer = new LinearLayout(context);
            footerContainer.setOrientation(LinearLayout.VERTICAL);
            footerContainer.setClipChildren(false);
            footerContainer.setClipToPadding(false);
            footerContainer.setPadding(0, footerVerticalPadding, 0, footerVerticalPadding);
            footerContainer.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                updateMessageViewportInset();
                refreshFrostedSurfaces();
            });
            conversationFrame.addView(footerContainer, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            ));

            LinearLayout inputRow = new LinearLayout(context);
            inputRow.setOrientation(LinearLayout.HORIZONTAL);
            inputRow.setGravity(Gravity.CENTER_VERTICAL);
            footerContainer.addView(inputRow, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            int floatingSurfaceColor = withAlpha(
                    ThemeUtils.getAdjustedBackgroundColor(false),
                    ThemeUtils.isDarkModeEnabled() ? 92 : 132
            );
            int floatingStrokeColor = blendColors(
                    ThemeUtils.getAdjustedBackgroundColor(false),
                    ThemeUtils.getAppForegroundColor(),
                    ThemeUtils.isDarkModeEnabled() ? 0.28f : 0.16f
            );

            inputSurface = createFrostedSurface(
                    context,
                    floatingSurfaceColor,
                    floatingStrokeColor,
                    6
            );
            inputView = new EditText(context);
            inputView.setHint(str("revanced_gemini_chat_input_hint"));
            inputView.setTextColor(ThemeUtils.getAppForegroundColor());
            inputView.setHintTextColor(withAlpha(ThemeUtils.getAppForegroundColor(), 150));
            inputView.setBackground(null);
            inputView.setFocusableInTouchMode(true);
            inputView.setSingleLine(true);
            inputView.setMinLines(1);
            inputView.setMaxLines(1);
            inputView.setTextSize(15);
            inputView.setMinHeight(dip42);
            inputView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            inputView.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            inputView.setPadding(dip16, dip10, dip16, dip10);
            inputView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            inputView.setVerticalScrollBarEnabled(false);
            LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                    0,
                    dip42,
                    1f
            );
            inputSurface.container.addView(inputView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            inputRow.addView(inputSurface.container, inputParams);

            sendSurface = createFrostedSurface(
                    context,
                    floatingSurfaceColor,
                    floatingStrokeColor,
                    8
            );
            sendButton = createIconButton(
                    context,
                    "revanced_gemini_send",
                    str("revanced_send")
            );
            sendButton.setBackground(null);
            LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(
                    dip42,
                    dip42
            );
            sendParams.setMarginStart(dip8);
            sendSurface.container.addView(sendButton, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            inputRow.addView(sendSurface.container, sendParams);
            footerContainer.post(() -> {
                updateMessageViewportInset();
                refreshFrostedSurfaces();
            });

            inputView.setOnClickListener(v -> {
                inputView.requestFocus();
                followConversationBottom = true;
                forceScrollToBottom();
            });
            inputView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    followConversationBottom = true;
                    forceScrollToBottom();
                }
            });

            sendButton.setOnClickListener(v -> {
                if (pendingUserQuestion != null) {
                    return;
                }

                String question = inputView.getText().toString().trim();
                if (TextUtils.isEmpty(question)) {
                    return;
                }

                inputView.setText("");
                beginPendingResponse(question);
                onSendListener.onSendRequested(this, question);
            });

            dialog = SheetBottomDialog.createSlideDialog(context, mainLayout, 180);
            dialog.setOnDismissListener(d -> {
                clearKeyboardInsetsListener();
                clearFrostedSurfaces();
                if (!dismissedByOwner) {
                    onUserDismiss.run();
                }
            });

            Window window = dialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            }

            dialog.show();
            applySheetFrame(mainLayout, 0);
            bindKeyboardInsets(mainLayout);
            messagesScrollView.post(() -> {
                updateCollapsingHeader();
                updateHeaderOverlay();
            });

            if (conversationHistory.isEmpty()) {
                scrollToTop();
            } else {
                forceScrollToBottom();
            }
        }

        boolean isShowing() {
            return dialog.isShowing();
        }

        void dismiss() {
            dismissedByOwner = true;
            clearKeyboardInsetsListener();
            dialog.dismiss();
        }

        void beginPendingResponse(@NonNull String question) {
            pendingUserQuestion = question;
            appendUserMessage(question, true);
            pendingAssistantEntry = appendAssistantMessage("", false, true, true);
            pendingAssistantEntry.messageView.setText("...");
            setSending(true);
        }

        @NonNull
        List<GeminiUtils.ChatMessage> getConversationWithPendingQuestion() {
            List<GeminiUtils.ChatMessage> snapshot = new ArrayList<>(conversationHistory);
            if (!TextUtils.isEmpty(pendingUserQuestion)) {
                snapshot.add(new GeminiUtils.ChatMessage("user", pendingUserQuestion));
            }
            return snapshot;
        }

        void updatePendingResponse(@NonNull String text) {
            if (pendingAssistantEntry == null) {
                return;
            }

            pendingAssistantEntry.rawText = text;
            if (TextUtils.isEmpty(text)) {
                pendingAssistantEntry.messageView.setText("...");
            } else {
                stopShimmerIfNeeded(pendingAssistantEntry.messageView);
                setMessageText(pendingAssistantEntry.messageView, assistantMessageFormatter.format(text));
            }
            maybeScrollToBottom(false);
        }

        void commitPendingResponse(@NonNull String text) {
            if (!TextUtils.isEmpty(pendingUserQuestion)) {
                conversationHistory.add(new GeminiUtils.ChatMessage("user", pendingUserQuestion));
                conversationHistory.add(new GeminiUtils.ChatMessage("model", text));
                onConversationChangedListener.onConversationChanged(new ArrayList<>(conversationHistory));
            }

            if (pendingAssistantEntry != null) {
                pendingAssistantEntry.rawText = text;
                stopShimmerIfNeeded(pendingAssistantEntry.messageView);
                setMessageText(pendingAssistantEntry.messageView, assistantMessageFormatter.format(text));
                if (pendingAssistantEntry.copyButton != null) {
                    pendingAssistantEntry.copyButton.setVisibility(View.VISIBLE);
                    pendingAssistantEntry.copyButton.setEnabled(true);
                }
            }

            pendingUserQuestion = null;
            pendingAssistantEntry = null;
            setSending(false);
            maybeScrollToBottom(false);
        }

        void failPendingResponse() {
            if (pendingAssistantEntry != null) {
                stopShimmerIfNeeded(pendingAssistantEntry.messageView);
            }
            if (pendingAssistantEntry != null && pendingAssistantEntry.row.getParent() instanceof ViewGroup parent) {
                parent.removeView(pendingAssistantEntry.row);
            }
            pendingUserQuestion = null;
            pendingAssistantEntry = null;
            setSending(false);
            maybeScrollToBottom(false);
        }

        private void setSending(boolean sending) {
            sendButton.setEnabled(!sending);
            inputView.setEnabled(!sending);
            sendButton.setAlpha(sending ? 0.55f : 1f);
            inputView.setAlpha(sending ? 0.7f : 1f);
        }

        private void appendSummaryMessage(
                @NonNull CharSequence text,
                @NonNull String rawText
        ) {
            final int dip6 = dipToPixels(6);
            final int dip8 = dipToPixels(8);
            final int dip36 = dipToPixels(36);

            LinearLayout section = new LinearLayout(context);
            section.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            sectionParams.topMargin = dip6;
            section.setLayoutParams(sectionParams);

            TextView messageView = createMessageView(false);
            messageView.setMaxWidth(Integer.MAX_VALUE);
            setMessageText(messageView, text);
            section.addView(messageView, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            ImageButton copyButton = createIconButton(
                    context,
                    "revanced_gemini_copy",
                    str("revanced_copy")
            );
            copyButton.setOnClickListener(v -> onCopyListener.onCopyRequested(rawText));
            LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(dip36, dip36);
            copyParams.topMargin = dip8;
            copyParams.gravity = Gravity.END;
            section.addView(copyButton, copyParams);

            messagesContainer.addView(section);
            maybeScrollToBottom(false);
        }

        private void appendUserMessage(@NonNull String rawText, boolean forceScroll) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.END);
            LinearLayout.LayoutParams rowParams = createMessageRowLayoutParams();
            row.setLayoutParams(rowParams);

            TextView messageView = createMessageView(true);
            setMessageText(messageView, rawText);
            row.addView(messageView);

            messagesContainer.addView(row);
            maybeScrollToBottom(forceScroll);
        }

        @NonNull
        private MessageEntry appendAssistantMessage(
                @NonNull String rawText,
                boolean showCopyButton,
                boolean forceScroll,
                boolean shimmerWhilePending
        ) {
            final int dip8 = dipToPixels(8);
            final int dip36 = dipToPixels(36);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.START | Gravity.BOTTOM);
            LinearLayout.LayoutParams rowParams = createMessageRowLayoutParams();
            row.setLayoutParams(rowParams);

            TextView messageView = createMessageView(false, shimmerWhilePending);
            if (TextUtils.isEmpty(rawText)) {
                messageView.setText("");
            } else {
                setMessageText(messageView, assistantMessageFormatter.format(rawText));
            }
            row.addView(messageView);

            ImageButton copyButton = createIconButton(
                    context,
                    "revanced_gemini_copy",
                    str("revanced_copy")
            );
            copyButton.setVisibility(showCopyButton ? View.VISIBLE : View.GONE);
            copyButton.setEnabled(showCopyButton);
            LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(dip36, dip36);
            copyParams.leftMargin = dip8;
            row.addView(copyButton, copyParams);

            MessageEntry entry = new MessageEntry(row, messageView, copyButton, rawText);
            copyButton.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(entry.rawText)) {
                    onCopyListener.onCopyRequested(entry.rawText);
                }
            });

            messagesContainer.addView(row);
            maybeScrollToBottom(forceScroll);
            return entry;
        }

        private void appendAssistantMessage(
                @NonNull String rawText
        ) {
            appendAssistantMessage(rawText, true, false, false);
        }

        @NonNull
        private LinearLayout.LayoutParams createMessageRowLayoutParams() {
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.topMargin = dipToPixels(6);
            return rowParams;
        }

        @NonNull
        private TextView createMessageView(boolean isUser, boolean shimmerWhilePending) {
            final int dip12 = dipToPixels(12);
            final int maxWidth = (int) (context.getResources().getDisplayMetrics().widthPixels * (isUser ? 0.74f : 0.68f));

            TextView messageView = shimmerWhilePending ? new ShimmerTextView(context) : new TextView(context);
            messageView.setMaxWidth(maxWidth);
            messageView.setTextSize(15);
            messageView.setTextColor(isUser
                    ? (ThemeUtils.isDarkModeEnabled() ? Color.BLACK : Color.WHITE)
                    : ThemeUtils.getAppForegroundColor());
            messageView.setBackground(createRoundedBackground(
                    isUser
                            ? getPrimaryButtonBackgroundColor()
                            : ThemeUtils.getAdjustedBackgroundColor(false),
                    18
            ));
            messageView.setPadding(dip12, dip12, dip12, dip12);
            if (shimmerWhilePending) {
                ShimmerTextView shimmerTextView = (ShimmerTextView) messageView;
                shimmerTextView.startShimmer();
            }
            return messageView;
        }

        @NonNull
        private TextView createMessageView(boolean isUser) {
            return createMessageView(isUser, false);
        }

        private void maybeScrollToBottom(boolean forceScroll) {
            if (forceScroll || followConversationBottom) {
                forceScrollToBottom();
            }
        }

        private void forceScrollToBottom() {
            followConversationBottom = true;
            messagesScrollView.post(() -> messagesScrollView.fullScroll(View.FOCUS_DOWN));
        }

        private void scrollToTop() {
            followConversationBottom = false;
            messagesScrollView.post(() -> messagesScrollView.scrollTo(0, 0));
        }

        private void updateCollapsingHeader() {
            float progress = resolveHeaderCollapseProgress();
            if (Math.abs(progress - headerCollapseProgress) < 0.001f) {
                return;
            }

            headerCollapseProgress = progress;
            if (summaryTitleSection != null) {
                summaryTitleSection.applyProgress(progress, headerCollapseTranslation);
            }
            if (summaryInfoSection != null) {
                summaryInfoSection.applyProgress(progress, headerCollapseTranslation);
            }

            setTopMargin(headerContainer, lerp(headerExpandedTopMargin, headerCollapsedTopMargin, progress));
            setBottomPadding(headerContainer, lerp(messagesExpandedTopMargin, messagesCollapsedTopMargin, progress));
            setTopMargin(
                    persistentHeaderView,
                    lerp(persistentHeaderExpandedTopMargin, persistentHeaderCollapsedTopMargin, progress)
            );

            if (collapsePersistentHeaderToSingleLine && persistentHeaderView.getVisibility() == View.VISIBLE) {
                boolean collapsed = progress >= 0.9f;
                persistentHeaderView.setMaxLines(collapsed ? 1 : Integer.MAX_VALUE);
                persistentHeaderView.setEllipsize(collapsed ? TextUtils.TruncateAt.END : null);
            }

            updateMessageViewportInset();
        }

        private float resolveHeaderCollapseProgress() {
            int collapseDistance = resolveHeaderCollapseDistance();
            if (collapseDistance <= 0) {
                return 0f;
            }
            return Math.max(0f, Math.min(1f, messagesScrollView.getScrollY() / (float) collapseDistance));
        }

        private int resolveHeaderCollapseDistance() {
            boolean hasCollapsibleHeaderContent = false;
            int collapseDistance = headerExpandedTopMargin - headerCollapsedTopMargin;
            collapseDistance += messagesExpandedTopMargin - messagesCollapsedTopMargin;
            collapseDistance += persistentHeaderExpandedTopMargin - persistentHeaderCollapsedTopMargin;

            if (summaryTitleSection != null && summaryTitleSection.isVisible()) {
                collapseDistance += summaryTitleSection.getExpandedTotalHeight();
                hasCollapsibleHeaderContent = true;
            }
            if (summaryInfoSection != null && summaryInfoSection.isVisible()) {
                collapseDistance += summaryInfoSection.getExpandedTotalHeight();
                hasCollapsibleHeaderContent = true;
            }

            if (!hasCollapsibleHeaderContent) {
                return 0;
            }
            return Math.max(dipToPixels(56), collapseDistance);
        }

        private void bindKeyboardInsets(@NonNull View targetView) {
            Window window = dialog.getWindow();
            if (window == null) {
                return;
            }

            keyboardObserverView = window.getDecorView();

            keyboardLayoutListener = () -> {
                View observerView = keyboardObserverView;
                if (observerView == null) {
                    return;
                }

                int keyboardInset = resolveKeyboardInset(observerView);
                if (keyboardInset == lastKeyboardInset) {
                    return;
                }

                lastKeyboardInset = keyboardInset;
                applySheetFrame(targetView, keyboardInset);

                if (keyboardInset > 0) {
                    inputView.requestFocus();
                    forceScrollToBottom();
                    Rect focusRect = new Rect(0, 0, inputView.getWidth(), inputView.getHeight());
                    inputView.requestRectangleOnScreen(focusRect, true);
                }
            };
            keyboardObserverView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
        }

        private void clearKeyboardInsetsListener() {
            if (keyboardObserverView == null || keyboardLayoutListener == null) {
                keyboardLayoutListener = null;
                keyboardObserverView = null;
                return;
            }

            ViewTreeObserver observer = keyboardObserverView.getViewTreeObserver();
            if (observer.isAlive()) {
                observer.removeOnGlobalLayoutListener(keyboardLayoutListener);
            }
            keyboardLayoutListener = null;
            keyboardObserverView = null;
        }

        private int resolveKeyboardInset(@NonNull View observerView) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsets insets = observerView.getRootWindowInsets();
                if (insets != null) {
                    return Math.max(0, insets.getInsets(WindowInsets.Type.ime()).bottom);
                }
            }

            Rect visibleFrame = new Rect();
            observerView.getWindowVisibleDisplayFrame(visibleFrame);
            int screenHeight = observerView.getResources().getDisplayMetrics().heightPixels;
            int keyboardHeight = Math.max(0, screenHeight - visibleFrame.bottom);
            int keyboardThreshold = screenHeight / 6;
            return keyboardHeight > keyboardThreshold ? keyboardHeight : 0;
        }

        private void applySheetFrame(@NonNull View targetView, int keyboardInset) {
            int screenHeight = targetView.getResources().getDisplayMetrics().heightPixels;
            int bottomInset = resolveBottomInset(targetView);
            int desiredHeight = (int) (screenHeight * SHEET_HEIGHT_FRACTION);
            int availableHeight = Math.max(
                    dipToPixels(MIN_SHEET_HEIGHT_DP),
                    screenHeight - bottomInset
            );
            int targetHeight = Math.min(desiredHeight, availableHeight);
            ViewGroup.LayoutParams layoutParams = targetView.getLayoutParams();
            if (layoutParams != null) {
                layoutParams.height = targetHeight;
                targetView.setLayoutParams(layoutParams);
            }
            int keyboardLiftPadding = Math.max(0, keyboardInset);
            targetView.setPadding(contentHorizontalPadding, 0, contentHorizontalPadding, 0);
            footerContainer.setPadding(
                    0,
                    footerVerticalPadding,
                    0,
                    footerVerticalPadding + keyboardLiftPadding
            );
            updateMessageViewportInset();

            Window window = dialog.getWindow();
            if (window == null) {
                return;
            }

            WindowManager.LayoutParams params = window.getAttributes();
            params.y = 0;
            window.setAttributes(params);
        }

        private void updateMessageViewportInset() {
            int headerInset = resolveHeaderInset();
            ViewGroup.LayoutParams headerSpacerLayoutParams = headerSpacerView.getLayoutParams();
            if (headerSpacerLayoutParams != null && headerSpacerLayoutParams.height != headerInset) {
                headerSpacerLayoutParams.height = headerInset;
                headerSpacerView.setLayoutParams(headerSpacerLayoutParams);
            }

            int footerInset = footerContainer.getHeight();
            if (footerInset <= 0) {
                footerInset = inputView.getMinHeight() + (footerVerticalPadding * 2);
            }

            int bottomPadding = footerInset + dipToPixels(12);
            if (messagesScrollView.getPaddingBottom() == bottomPadding) {
                return;
            }

            messagesScrollView.setPadding(0, 0, 0, bottomPadding);
            refreshFrostedSurfaces();
        }

        private int resolveHeaderInset() {
            int headerHeight = headerContainer.getHeight();
            if (headerHeight <= 0) {
                headerHeight = headerContainer.getMeasuredHeight();
            }
            return Math.max(0, headerHeight + getTopMargin(headerContainer));
        }

        private void updateHeaderOverlay() {
            int overlayStart = headerContainer.getPaddingBottom();
            int targetAlpha = Math.round(
                    headerOverlayMaxAlpha * Math.max(
                            0f,
                            Math.min(
                                    1f,
                                    (messagesScrollView.getScrollY() - overlayStart) / (float) headerOverlayFadeDistance
                            )
                    )
            );
            if (headerOverlayBackground.getAlpha() == targetAlpha) {
                return;
            }

            headerOverlayBackground.setAlpha(targetAlpha);
            headerContainer.invalidate();
        }

        private void refreshFrostedSurfaces() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return;
            }

            inputSurface.refresh(messagesScrollView);
            sendSurface.refresh(messagesScrollView);
        }

        private void clearFrostedSurfaces() {
            inputSurface.clear();
            sendSurface.clear();
        }

        private int resolveBottomInset(@NonNull View targetView) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsets insets = targetView.getRootWindowInsets();
                if (insets != null) {
                    return Math.max(0, insets.getInsets(WindowInsets.Type.systemBars()).bottom);
                }
            }

            Rect visibleFrame = new Rect();
            targetView.getWindowVisibleDisplayFrame(visibleFrame);
            int screenHeight = targetView.getResources().getDisplayMetrics().heightPixels;
            int obscuredBottom = Math.max(0, screenHeight - visibleFrame.bottom);
            int keyboardThreshold = screenHeight / 6;
            return obscuredBottom > keyboardThreshold ? 0 : obscuredBottom;
        }

        private void stopShimmerIfNeeded(@NonNull TextView textView) {
            if (textView instanceof ShimmerTextView shimmerTextView) {
                shimmerTextView.stopShimmer();
            }
        }
    }

    @NonNull
    static SheetBottomDialog.SlideDialog showTranscriptionSheet(
            @NonNull Context context,
            @Nullable String metaText,
            @Nullable String infoText,
            @NonNull String rawTranscription,
            @NonNull Runnable onCopy,
            @NonNull Runnable onSubtitles
    ) {
        final int dip16 = dipToPixels(16);
        final int dip20 = dipToPixels(20);

        SheetBottomDialog.DraggableLinearLayout mainLayout =
                SheetBottomDialog.createMainLayout(context, ThemeUtils.getDialogBackgroundColor());
        mainLayout.setPadding(dip20, 0, dip20, dip20);
        mainLayout.setMinimumHeight((int) (context.getResources().getDisplayMetrics().heightPixels * SHEET_HEIGHT_FRACTION));

        TextView titleView = createTitleView(context, str("revanced_gemini_transcription_result_title"));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = dip20;
        mainLayout.addView(titleView, titleParams);

        TextView metaView = createMetaView(context);
        mainLayout.addView(metaView);
        setTextOrHide(metaView, metaText);

        TextView infoView = createSecondaryTextView(context);
        mainLayout.addView(infoView);
        setTextOrHide(infoView, infoText);

        MaxHeightScrollView bodyScroll = new MaxHeightScrollView(context);
        bodyScroll.setMaxHeight((int) (context.getResources().getDisplayMetrics().heightPixels * 0.72f));
        bodyScroll.setVerticalScrollBarEnabled(false);
        bodyScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        bodyParams.topMargin = dip16;

        TextView bodyView = new TextView(context);
        bodyView.setText(rawTranscription);
        bodyView.setTextColor(ThemeUtils.getAppForegroundColor());
        bodyView.setTextSize(15);
        bodyView.setPadding(dip16, dip16, dip16, dip16);
        bodyView.setBackground(createRoundedBackground(
                ThemeUtils.getAdjustedBackgroundColor(false),
                18
        ));
        bodyScroll.addView(bodyView, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));
        mainLayout.addView(bodyScroll, bodyParams);

        LinearLayout buttonsRow = createButtonsRow(context);
        Button cancelButton = createActionButton(context, str("revanced_cancel"), false);
        Button copyButton = createActionButton(context, str("revanced_copy"), false);
        Button subtitlesButton = createActionButton(context, str("revanced_gemini_transcription_parse_button"), true);

        final SheetBottomDialog.SlideDialog dialog = SheetBottomDialog.createSlideDialog(context, mainLayout, 180);
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        copyButton.setOnClickListener(v -> onCopy.run());
        subtitlesButton.setOnClickListener(v -> {
            onSubtitles.run();
            dialog.dismiss();
        });

        addWeightedButton(buttonsRow, cancelButton);
        addWeightedButton(buttonsRow, copyButton);
        addWeightedButton(buttonsRow, subtitlesButton);
        mainLayout.addView(buttonsRow);

        dialog.show();
        applyStaticSheetFrame(dialog, mainLayout);
        return dialog;
    }

    @NonNull
    private static TextView createTitleView(@NonNull Context context, @NonNull String title) {
        TextView view = new TextView(context);
        view.setText(title);
        view.setTextSize(18);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER_HORIZONTAL);
        view.setTextColor(ThemeUtils.getAppForegroundColor());
        return view;
    }

    @NonNull
    private static TextView createMetaView(@NonNull Context context) {
        TextView view = new TextView(context);
        view.setTextColor(ThemeUtils.getAppForegroundColor());
        view.setTextSize(14);
        view.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dipToPixels(8);
        view.setLayoutParams(params);
        return view;
    }

    @NonNull
    private static TextView createSecondaryTextView(@NonNull Context context) {
        TextView view = new TextView(context);
        view.setTextColor(withAlpha(ThemeUtils.getAppForegroundColor(), 200));
        view.setTextSize(13);
        view.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dipToPixels(4);
        view.setLayoutParams(params);
        return view;
    }

    @NonNull
    private static LinearLayout createButtonsRow(@NonNull Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dipToPixels(16);
        row.setLayoutParams(params);
        return row;
    }

    private static void addWeightedButton(@NonNull LinearLayout row, @NonNull Button button) {
        final int dip4 = dipToPixels(4);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dipToPixels(42),
                1f
        );
        params.leftMargin = dip4;
        params.rightMargin = dip4;
        row.addView(button, params);
    }

    @NonNull
    private static Button createActionButton(@NonNull Context context, @NonNull String text, boolean primary) {
        Button button = new Button(context, null, 0);
        button.setText(text);
        button.setAllCaps(false);
        button.setSingleLine(true);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(14);
        button.setPadding(dipToPixels(16), 0, dipToPixels(16), 0);
        button.setBackground(createRoundedBackground(
                primary ? getPrimaryButtonBackgroundColor() : SECONDARY_BUTTON_BACKGROUND_COLOR,
                20
        ));
        button.setTextColor(primary
                ? (ThemeUtils.isDarkModeEnabled() ? Color.BLACK : Color.WHITE)
                : ThemeUtils.getAppForegroundColor());
        return button;
    }

    @NonNull
    private static ImageButton createIconButton(
            @NonNull Context context,
            @NonNull String drawableName,
            @NonNull String contentDescription
    ) {
        final int iconPadding = dipToPixels(5);

        ImageButton button = new ImageButton(context);
        button.setBackground(createRoundedBackground(
                SECONDARY_BUTTON_BACKGROUND_COLOR,
                20
        ));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
        button.setContentDescription(contentDescription);

        Drawable drawable = getDrawable(drawableName);
        if (drawable != null) {
            drawable = drawable.mutate();
            drawable.setTint(ThemeUtils.getAppForegroundColor());
            button.setImageDrawable(drawable);
        }
        return button;
    }

    private static void setTextOrHide(@NonNull TextView textView, @Nullable String text) {
        if (TextUtils.isEmpty(text)) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
            textView.setText(text);
        }
    }

    private static void setMessageText(@NonNull TextView textView, @NonNull CharSequence text) {
        textView.setText(text);
        if (text instanceof Spanned) {
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setLinksClickable(true);
        } else {
            textView.setMovementMethod(null);
        }
    }

    private static void setTopMargin(@NonNull View view, int topMargin) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (!(layoutParams instanceof ViewGroup.MarginLayoutParams marginLayoutParams)
                || marginLayoutParams.topMargin == topMargin) {
            return;
        }

        marginLayoutParams.topMargin = topMargin;
        view.setLayoutParams(marginLayoutParams);
    }

    private static int getTopMargin(@NonNull View view) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams marginLayoutParams) {
            return marginLayoutParams.topMargin;
        }
        return 0;
    }

    private static void setBottomPadding(@NonNull View view, int bottomPadding) {
        if (view.getPaddingBottom() == bottomPadding) {
            return;
        }

        view.setPadding(
                view.getPaddingLeft(),
                view.getPaddingTop(),
                view.getPaddingRight(),
                bottomPadding
        );
    }

    private static int lerp(int start, int end, float progress) {
        return Math.round(start + ((end - start) * progress));
    }

    @NonNull
    private static ShapeDrawable createRoundedBackground(int color, float radiusDp) {
        ShapeDrawable background = new ShapeDrawable(new RoundRectShape(
                Utils.createCornerRadii(radiusDp), null, null
        ));
        background.getPaint().setColor(color);
        return background;
    }

    @NonNull
    private static Drawable createFloatingBackground(int strokeColor) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dipToPixels((float) 20.0));
        background.setColor(Color.TRANSPARENT);
        background.setStroke(Math.max(1, dipToPixels(1)), strokeColor);
        return background;
    }

    @NonNull
    private static Drawable createFloatingOutlineBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dipToPixels((float) 20.0));
        background.setColor(Color.TRANSPARENT);
        return background;
    }

    @NonNull
    private static Drawable createHeaderOverlayBackground(int color) {
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{
                        color,
                        color,
                        withAlpha(color, 0)
                }
        );
    }

    @NonNull
    @SuppressWarnings("SuspiciousNameCombination")
    private static FrostedSurface createFrostedSurface(
            @NonNull Context context,
            int overlayColor,
            int strokeColor,
            float elevationDp
    ) {
        int strokeWidth = Math.max(1, dipToPixels(1));
        FrameLayout container = new FrameLayout(context);
        container.setClipChildren(true);
        container.setClipToPadding(true);
        container.setClipToOutline(true);
        container.setBackground(createFloatingOutlineBackground());
        container.setElevation(dipToPixels(elevationDp));
        container.setTranslationZ(dipToPixels(elevationDp));

        ImageView blurView = new ImageView(context);
        blurView.setScaleType(ImageView.ScaleType.FIT_XY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            float blurRadius = dipToPixels(18);
            blurView.setRenderEffect(RenderEffect.createBlurEffect(
                    blurRadius,
                    blurRadius,
                    Shader.TileMode.CLAMP
            ));
        } else {
            blurView.setVisibility(View.GONE);
        }
        container.addView(blurView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        FrameLayout.LayoutParams blurParams = (FrameLayout.LayoutParams) blurView.getLayoutParams();
        blurParams.setMargins(strokeWidth, strokeWidth, strokeWidth, strokeWidth);
        blurView.setLayoutParams(blurParams);

        View scrimView = new View(context);
        scrimView.setBackground(createRoundedBackground(overlayColor, (float) 20));
        container.addView(scrimView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        FrameLayout.LayoutParams scrimParams = (FrameLayout.LayoutParams) scrimView.getLayoutParams();
        scrimParams.setMargins(strokeWidth, strokeWidth, strokeWidth, strokeWidth);
        scrimView.setLayoutParams(scrimParams);

        View borderView = new View(context);
        borderView.setBackground(createFloatingBackground(strokeColor));
        container.addView(borderView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        return new FrostedSurface(container, blurView);
    }

    private static int getPrimaryButtonBackgroundColor() {
        return ThemeUtils.isDarkModeEnabled() ? Color.WHITE : Color.BLACK;
    }

    private static int blendColors(int baseColor, int accentColor, float accentRatio) {
        float clampedRatio = Math.max(0f, Math.min(1f, accentRatio));
        float baseRatio = 1f - clampedRatio;
        return Color.rgb(
                Math.round((Color.red(baseColor) * baseRatio) + (Color.red(accentColor) * clampedRatio)),
                Math.round((Color.green(baseColor) * baseRatio) + (Color.green(accentColor) * clampedRatio)),
                Math.round((Color.blue(baseColor) * baseRatio) + (Color.blue(accentColor) * clampedRatio))
        );
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static void applyStaticSheetFrame(
            @NonNull SheetBottomDialog.SlideDialog dialog,
            @NonNull View targetView
    ) {
        int screenHeight = targetView.getResources().getDisplayMetrics().heightPixels;
        int bottomInset = resolveBottomInset(targetView);
        int targetHeight = Math.min(
                (int) (screenHeight * SHEET_HEIGHT_FRACTION),
                screenHeight - bottomInset
        );
        ViewGroup.LayoutParams layoutParams = targetView.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.height = targetHeight;
            targetView.setLayoutParams(layoutParams);
        }
        targetView.setMinimumHeight(targetHeight);

        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }

        WindowManager.LayoutParams params = window.getAttributes();
        params.y = 0;
        window.setAttributes(params);
    }

    private static int resolveBottomInset(@NonNull View targetView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsets insets = targetView.getRootWindowInsets();
            if (insets != null) {
                return Math.max(0, insets.getInsets(WindowInsets.Type.systemBars()).bottom);
            }
        }

        Rect visibleFrame = new Rect();
        targetView.getWindowVisibleDisplayFrame(visibleFrame);
        int screenHeight = targetView.getResources().getDisplayMetrics().heightPixels;
        int obscuredBottom = Math.max(0, screenHeight - visibleFrame.bottom);
        int keyboardThreshold = screenHeight / 6;
        return obscuredBottom > keyboardThreshold ? 0 : obscuredBottom;
    }

    private static final class MessageEntry {
        private final LinearLayout row;
        private final TextView messageView;
        @Nullable
        private final ImageButton copyButton;
        @NonNull
        private String rawText;

        private MessageEntry(
                @NonNull LinearLayout row,
                @NonNull TextView messageView,
                @Nullable ImageButton copyButton,
                @NonNull String rawText
        ) {
            this.row = row;
            this.messageView = messageView;
            this.copyButton = copyButton;
            this.rawText = rawText;
        }
    }

    private static final class CollapsibleHeaderSection {
        private final FrameLayout container;
        private final View contentView;
        private final int expandedTopMargin;
        private int expandedHeight = -1;

        private CollapsibleHeaderSection(
                @NonNull FrameLayout container,
                @NonNull View contentView,
                int expandedTopMargin
        ) {
            this.container = container;
            this.contentView = contentView;
            this.expandedTopMargin = expandedTopMargin;
        }

        @NonNull
        private static CollapsibleHeaderSection create(
                @NonNull Context context,
                @NonNull View contentView,
                int topMargin
        ) {
            FrameLayout container = new FrameLayout(context);
            container.setClipChildren(true);
            container.setClipToPadding(true);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = topMargin;
            container.setLayoutParams(params);
            container.addView(contentView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            ));
            return new CollapsibleHeaderSection(container, contentView, topMargin);
        }

        private boolean isVisible() {
            return container.getVisibility() == View.VISIBLE && contentView.getVisibility() == View.VISIBLE;
        }

        private int getExpandedTotalHeight() {
            ensureExpandedHeight();
            return expandedHeight > 0 ? expandedHeight + expandedTopMargin : 0;
        }

        private void applyProgress(float progress, int translationY) {
            if (!isVisible()) {
                return;
            }

            ensureExpandedHeight();
            if (expandedHeight <= 0) {
                contentView.setAlpha(1f - progress);
                contentView.setTranslationY(-translationY * progress);
                return;
            }

            ViewGroup.LayoutParams layoutParams = container.getLayoutParams();
            if (layoutParams instanceof ViewGroup.MarginLayoutParams marginLayoutParams) {
                int targetTopMargin = Math.max(0, Math.round(expandedTopMargin * (1f - progress)));
                int targetHeight = progress <= 0f
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : Math.max(0, Math.round(expandedHeight * (1f - progress)));
                if (marginLayoutParams.topMargin != targetTopMargin || marginLayoutParams.height != targetHeight) {
                    marginLayoutParams.topMargin = targetTopMargin;
                    marginLayoutParams.height = targetHeight;
                    container.setLayoutParams(marginLayoutParams);
                }
            }

            contentView.setAlpha(1f - progress);
            contentView.setTranslationY(-translationY * progress);
        }

        private void ensureExpandedHeight() {
            if (expandedHeight > 0) {
                return;
            }
            if (contentView.getHeight() > 0) {
                expandedHeight = contentView.getHeight();
                return;
            }

            int width = container.getWidth();
            if (width <= 0) {
                width = contentView.getWidth();
            }
            if (width <= 0) {
                return;
            }

            contentView.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            expandedHeight = contentView.getMeasuredHeight();
        }
    }

    private static final class MaxHeightScrollView extends ScrollView {
        interface OnScrollPositionChangedListener {
            void onScrollPositionChanged();
        }

        private int maxHeight = Integer.MAX_VALUE;
        @Nullable
        private OnScrollPositionChangedListener scrollPositionChangedListener;

        private MaxHeightScrollView(@NonNull Context context) {
            super(context);
        }

        void setMaxHeight(int maxHeight) {
            this.maxHeight = maxHeight;
        }

        void setOnScrollPositionChangedListener(@Nullable OnScrollPositionChangedListener listener) {
            scrollPositionChangedListener = listener;
        }

        boolean isNearBottom(int thresholdPx) {
            View child = getChildCount() > 0 ? getChildAt(0) : null;
            if (child == null) {
                return true;
            }

            int distanceToBottom = child.getBottom() - (getScrollY() + getHeight());
            return distanceToBottom <= thresholdPx;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int cappedHeightSpec = heightMeasureSpec;
            if (maxHeight < Integer.MAX_VALUE) {
                int heightMode = MeasureSpec.getMode(heightMeasureSpec);
                int heightSize = MeasureSpec.getSize(heightMeasureSpec);
                if (heightMode == MeasureSpec.UNSPECIFIED) {
                    cappedHeightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
                } else {
                    cappedHeightSpec = MeasureSpec.makeMeasureSpec(
                            Math.min(heightSize, maxHeight),
                            heightMode
                    );
                }
            }
            super.onMeasure(widthMeasureSpec, cappedHeightSpec);
        }

        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt) {
            super.onScrollChanged(l, t, oldl, oldt);
            if (scrollPositionChangedListener != null) {
                scrollPositionChangedListener.onScrollPositionChanged();
            }
        }
    }

    @SuppressLint("AppCompatCustomView")
    private static final class ShimmerTextView extends TextView {
        @Nullable
        private ValueAnimator shimmerAnimator;
        @Nullable
        private LinearGradient shimmerGradient;
        private final Matrix shimmerMatrix = new Matrix();

        private ShimmerTextView(@NonNull Context context) {
            super(context);
        }

        void startShimmer() {
            if (shimmerAnimator != null) {
                return;
            }
            shimmerAnimator = ValueAnimator.ofFloat(-1f, 2f);
            shimmerAnimator.setDuration(1300L);
            shimmerAnimator.setInterpolator(new LinearInterpolator());
            shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
            shimmerAnimator.addUpdateListener(animation -> {
                if (shimmerGradient == null || getWidth() <= 0) {
                    return;
                }
                float animatedFraction = (float) animation.getAnimatedValue();
                shimmerMatrix.setTranslate(getWidth() * animatedFraction, 0f);
                shimmerGradient.setLocalMatrix(shimmerMatrix);
                invalidate();
            });
            rebuildShader();
            shimmerAnimator.start();
        }

        void stopShimmer() {
            if (shimmerAnimator != null) {
                shimmerAnimator.cancel();
                shimmerAnimator = null;
            }
            getPaint().setShader(null);
            invalidate();
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            rebuildShader();
        }

        private void rebuildShader() {
            if (getWidth() <= 0) {
                return;
            }

            int baseColor = getCurrentTextColor();
            int dimColor = withAlpha(baseColor, 105);
            int brightColor = withAlpha(baseColor, 255);
            shimmerGradient = new LinearGradient(
                    -getWidth(),
                    0f,
                    0f,
                    0f,
                    new int[]{dimColor, brightColor, dimColor},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            );
            getPaint().setShader(shimmerGradient);
            invalidate();
        }
    }

    private static final class FrostedSurface {
        private final FrameLayout container;
        private final ImageView blurView;
        @Nullable
        private Bitmap blurBitmap;

        private FrostedSurface(@NonNull FrameLayout container, @NonNull ImageView blurView) {
            this.container = container;
            this.blurView = blurView;
        }

        private void refresh(@NonNull View sourceView) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return;
            }

            int width = container.getWidth();
            int height = container.getHeight();
            if (width <= 0 || height <= 0 || sourceView.getWidth() <= 0 || sourceView.getHeight() <= 0) {
                return;
            }

            View drawableSource = sourceView;
            if (sourceView instanceof ViewGroup sourceGroup && sourceGroup.getChildCount() > 0) {
                drawableSource = sourceGroup.getChildAt(0);
            }

            int[] sourceLocation = new int[2];
            int[] targetLocation = new int[2];
            drawableSource.getLocationInWindow(sourceLocation);
            container.getLocationInWindow(targetLocation);

            Bitmap bitmap = ensureBitmap(width, height);
            bitmap.eraseColor(Color.TRANSPARENT);

            Canvas canvas = new Canvas(bitmap);
            canvas.translate(
                    sourceLocation[0] - targetLocation[0],
                    sourceLocation[1] - targetLocation[1]
            );
            drawableSource.draw(canvas);
            blurView.setImageBitmap(bitmap);
            blurView.invalidate();
        }

        private void clear() {
            blurView.setImageDrawable(null);
            if (blurBitmap != null) {
                blurBitmap.recycle();
                blurBitmap = null;
            }
        }

        @NonNull
        private Bitmap ensureBitmap(int width, int height) {
            if (blurBitmap == null || blurBitmap.getWidth() != width || blurBitmap.getHeight() != height) {
                if (blurBitmap != null) {
                    blurBitmap.recycle();
                }
                blurBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }
            return blurBitmap;
        }
    }
}
