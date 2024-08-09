package ets.acmi.gnssdislogger.ui;


import android.app.Activity;
import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.core.text.HtmlCompat;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.ms.square.android.expandabletextview.ExpandableTextView;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.Strings;
import ets.acmi.gnssdislogger.loggers.Files;

public class Dialogs {
    private static MaterialDialog pd;

    private static String getFormattedErrorMessageForDisplay(String message, Throwable throwable) {
        StringBuilder html = new StringBuilder();
        if (!Strings.isNullOrEmpty(message)) {
            html.append("<b>").append(message.replace("\r\n", "<br />")
                    .replace("\n", "<br />")).append("</b> <br /><br />");
        }

        while (throwable != null && !Strings.isNullOrEmpty(throwable.getMessage())) {
            html.append(throwable.getMessage().replace("\r\n", "<br />"))
                    .append("<br /><br />");
            throwable = throwable.getCause();
        }

        return html.toString();
    }

    private static String getFormattedErrorMessageForPlainText(String message, Throwable throwable) {

        StringBuilder sb = new StringBuilder();
        if (!Strings.isNullOrEmpty(message)) {
            sb.append(message).append("\r\n");
        }

        while (throwable != null && !Strings.isNullOrEmpty(throwable.getMessage())) {
            sb.append("\r\n\r\n").append(throwable.getMessage()).append("\r\n");
            if (throwable.getStackTrace().length > 0) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                sb.append(sw.toString());
            }
            throwable = throwable.getCause();
        }

        return sb.toString();

    }

    public static void error(String title, final String friendlyMessage, final String errorMessage, final Throwable throwable, final Context context) {

        final String messageFormatted = getFormattedErrorMessageForDisplay(errorMessage, throwable);

        MaterialDialog alertDialog = new MaterialDialog.Builder(context)
                .title(title)
                .customView(R.layout.error_alertview, true)
                .autoDismiss(false)
                .negativeText("Copy")
                .positiveText(R.string.ok)
                .neutralText("Details")
                .onNeutral((materialDialog, dialogAction) -> {

                    final ExpandableTextView expTv1 = materialDialog.getCustomView().findViewById(R.id.error_expand_text_view);
                    expTv1.findViewById(R.id.expand_collapse).performClick();

                })
                .onNegative((materialDialog, dialogAction) -> {

                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("GpsLogger error message", getFormattedErrorMessageForPlainText(friendlyMessage, throwable));
                    clipboard.setPrimaryClip(clip);

                    materialDialog.dismiss();
                })
                .onPositive((materialDialog, dialogAction) -> materialDialog.dismiss())
                .build();


        final ExpandableTextView expTv1 = alertDialog.getCustomView().findViewById(R.id.error_expand_text_view);
        expTv1.setText(HtmlCompat.fromHtml(messageFormatted, HtmlCompat.FROM_HTML_MODE_LEGACY));
        TextView tv = expTv1.findViewById(R.id.expandable_text);
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        TextView tvFriendly = alertDialog.getCustomView().findViewById(R.id.error_friendly_message);
        tvFriendly.setText(friendlyMessage);

        if (context instanceof Activity && !((Activity) context).isFinishing()) {
            alertDialog.show();
        } else {
            alertDialog.show();
        }
    }

    /**
     * Displays a message box to the user with an OK button.
     *
     * @param title
     * @param message
     * @param context The calling class, such as GnssMainActivity.this or
     *                mainActivity.
     */
    public static void alert(String title, String message, Context context) {
        alert(title, message, context, false, null);
    }


    public static void alert(String title, String message, Context context, final MessageBoxCallback msgCallback) {
        alert(title, message, context, false, msgCallback);
    }

    /**
     * Displays a message box to the user with an OK button.
     *
     * @param title
     * @param message
     * @param context     The calling class, such as GnssMainActivity.this or
     *                    mainActivity.
     * @param msgCallback An object which implements IHasACallBack so that the
     *                    click event can call the callback method.
     */
    public static void alert(String title, String message, Context context, boolean includeCancel, final MessageBoxCallback msgCallback) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                .title(title)
                .content(HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY))
                .positiveText(R.string.ok)

                .onPositive((materialDialog, dialogAction) -> {
                    if (msgCallback != null) {
                        msgCallback.messageBoxResult(MessageBoxCallback.OK);
                    }
                });

        if (includeCancel) {
            builder.negativeText(R.string.cancel);
            builder.onNegative((materialDialog, dialogAction) -> {
                if (msgCallback != null) {
                    msgCallback.messageBoxResult(MessageBoxCallback.CANCEL);
                }
            });
        }


        MaterialDialog alertDialog = builder.build();

        if (context instanceof Activity && !((Activity) context).isFinishing()) {
            alertDialog.show();
        } else {
            alertDialog.show();
        }

    }

    public static void progress(Context context, String title, String message) {
        if (context != null) {

            pd = new MaterialDialog.Builder(context)
                    .title(title)
                    .content(message)
                    .progress(true, 0)
                    .show();
        }
    }

    public static void hideProgress() {
        if (pd != null) {
            pd.dismiss();
        }
    }


    /**
     * Text input dialog, with auto complete entries stored in cache.
     * Invokes callback with user entry afterwards, only if OK is pressed
     * Dismisses dialog if no text is entered
     *
     * @param cacheKey the unique cache key for this dialog's entries
     * @param title    the title of the dialog box
     * @param hint     the hint to show if text is empty
     * @param text     the text to set in the text box
     * @param callback the callback to invoke after user presses OK
     */
    public static void autoCompleteText(final Context ctx, final String cacheKey, String title,
                                        String hint, String text,
                                        final AutoCompleteCallback callback) {

        final List<String> cachedList = Files.getListFromCacheFile(cacheKey, ctx);
        final LinkedHashSet<String> set = new LinkedHashSet(cachedList);

        final MaterialDialog alertDialog = new MaterialDialog.Builder(ctx)
                .title(title)
                .customView(R.layout.custom_autocomplete_view, true)
                .negativeText(R.string.cancel)
                .positiveText(R.string.ok)
                .onPositive((materialDialog, dialogAction) -> {

                    AutoCompleteTextView autoComplete = materialDialog.getCustomView().findViewById(R.id.custom_autocomplete);
                    String enteredText = autoComplete.getText().toString();

                    if (Strings.isNullOrEmpty(enteredText)) {
                        materialDialog.dismiss();
                        return;
                    } else if (set.add(enteredText)) {
                        Files.saveListToCacheFile(new ArrayList<>(set), cacheKey, ctx);
                    }

                    callback.messageBoxResult(MessageBoxCallback.OK, materialDialog, enteredText);
                    materialDialog.dismiss();
                })
                .onNegative((materialDialog, dialogAction) -> {
                    callback.messageBoxResult(MessageBoxCallback.CANCEL, materialDialog, "");
                    materialDialog.dismiss();
                })
                .build();

        String[] arr = set.toArray(new String[0]);

        final AutoCompleteTextView customAutocomplete = alertDialog.getCustomView().findViewById(R.id.custom_autocomplete);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx, android.R.layout.simple_dropdown_item_1line, arr);
        customAutocomplete.setAdapter(adapter);
        customAutocomplete.setHint(hint);
        customAutocomplete.append(text);

        // set keyboard done as dialog positive
        customAutocomplete.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                alertDialog.getActionButton(DialogAction.POSITIVE).callOnClick();

            }
            return false;
        });

        // show autosuggest dropdown even if empty
        customAutocomplete.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                customAutocomplete.showDropDown();
                customAutocomplete.requestFocus();
            }
        });

        // show keyboard
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();

    }

    public interface MessageBoxCallback {

        int CANCEL = 0;
        int OK = 1;

        void messageBoxResult(int which);
    }

    public interface AutoCompleteCallback {
        int CANCEL = 0;
        int OK = 1;

        void messageBoxResult(int which, MaterialDialog dialog, String enteredText);
    }
}
