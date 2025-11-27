// java
package com.example.nt118_englishvocabapp.util;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;

import androidx.fragment.app.FragmentActivity;

import com.example.nt118_englishvocabapp.R;

public final class KeyboardUtils {
    private static final String TAG = "KeyboardUtils";

    private KeyboardUtils() { /* no instances */ }

    /**
     * Hide keyboard, remove provided global-layout listener (if any), restore bottom UI.
     * Returns null so callers can assign: keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(...);
     */
    public static ViewTreeObserver.OnGlobalLayoutListener hideKeyboardAndRestoreUI(
            Activity activity,
            View anchor,
            View keyboardRootView,
            ViewTreeObserver.OnGlobalLayoutListener keyboardListener) {

        if (activity == null) return null;

        // 1) Hide keyboard using provided anchor or activity decor view
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        View tokenSource = (anchor != null) ? anchor : activity.getWindow().getDecorView();
        try {
            if (imm != null && tokenSource != null) {
                imm.hideSoftInputFromWindow(tokenSource.getWindowToken(), 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to hide keyboard", e);
        }

        // 2) Remove keyboard global layout listener if attached
        if (keyboardRootView != null && keyboardListener != null) {
            try {
                keyboardRootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
            } catch (Exception e) {
                Log.e(TAG, "Failed to remove keyboard listener", e);
            }
            keyboardListener = null;
        }

        // 3) Restore bottom bar and FAB visibility
        try {
            View bottomAppBar = activity.findViewById(R.id.bottomAppBar);
            View fab = activity.findViewById(R.id.fab);
            if (bottomAppBar != null) bottomAppBar.setVisibility(View.VISIBLE);
            if (fab != null) fab.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore bottom UI", e);
        }

        return null;
    }

    /**
     * Show soft keyboard for the given target view (or activity decor view if target is null).
     * Requests focus on the target and performs showSoftInput on the UI thread.
     */
    public static void showKeyboard(Activity activity, View target) {
        if (activity == null) return;

        final View tokenView = (target != null) ? target : activity.getWindow().getDecorView();

        try {
            if (target != null) {
                target.requestFocus();
            } else {
                tokenView.requestFocus();
            }

            final InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm == null || tokenView == null) return;

            // Post to ensure focus is set and view is attached before requesting keyboard
            tokenView.post(() -> {
                try {
                    imm.showSoftInput(tokenView, InputMethodManager.SHOW_IMPLICIT);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to show keyboard", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error while attempting to show keyboard", e);
        }
    }

    public static void hideKeyboard(Activity activity) {
        if (activity == null) return;
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        // Tìm view đang được focus để lấy token
        View view = activity.getCurrentFocus();
        // Nếu không có view nào focus, tạo mới để tránh lỗi null
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
