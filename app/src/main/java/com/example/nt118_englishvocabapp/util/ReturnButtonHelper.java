package com.example.nt118_englishvocabapp.util;

import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.R;

public final class ReturnButtonHelper {
    private ReturnButtonHelper() {}

    /**
     * Binds the standard back behavior to any ImageButton with id R.id.btn_return in the provided root view.
     * Behavior: if the parent FragmentManager has entries on the back stack, pop it; otherwise trigger
     * the activity's onBackPressedDispatcher to let the host decide (will finish activity by default).
     */
    public static void bind(@NonNull View root, @NonNull Fragment fragment) {
        bind(root, fragment, null, null);
    }

    /**
     * Binds the back behavior to R.id.btn_return. If fallback is provided and the fragment manager has no
     * back stack entries, the fallback Runnable will be executed instead of calling onBackPressed.
     */
    public static void bind(@NonNull View root, @NonNull Fragment fragment, Runnable fallback) {
        bind(root, fragment, null, fallback);
    }

    /**
     * Binds the back behavior to R.id.btn_return.
     * preClick will be executed first (if non-null) with the clicked view; this is useful to hide keyboard or
     * perform other cleanup that must run before the fragment manager pop.
     * If the fragment manager has back stack entries, they are popped. Otherwise, fallback (if provided)
     * is executed; if fallback is null, onBackPressed is dispatched.
     */
    public static void bind(@NonNull View root, @NonNull Fragment fragment, View.OnClickListener preClick, Runnable fallback) {
        ImageButton btn = root.findViewById(R.id.btn_return);
        if (btn == null) return;

        btn.setOnClickListener(v -> {
            // Run pre-click cleanup if provided
            if (preClick != null) {
                try {
                    preClick.onClick(v);
                } catch (Exception ignored) {}
            }

            int count = fragment.getParentFragmentManager().getBackStackEntryCount();
            if (count > 0) {
                fragment.getParentFragmentManager().popBackStack();
            } else if (fallback != null) {
                try {
                    fallback.run();
                } catch (Exception ignored) {
                    fragment.requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            } else {
                fragment.requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    /**
     * Perform the same return/back behavior without binding to a view.
     * Useful for arbitrary buttons that should follow the same logic.
     * preAction runs first (nullable). If the fragment manager has back entries, popBackStack()
     * is used; otherwise fallback Runnable is run (if provided) or onBackPressed is dispatched.
     */
    public static void performReturn(@NonNull Fragment fragment, Runnable preAction, Runnable fallback) {
        if (preAction != null) {
            try { preAction.run(); } catch (Exception ignored) {}
        }

        int count = fragment.getParentFragmentManager().getBackStackEntryCount();
        if (count > 0) {
            fragment.getParentFragmentManager().popBackStack();
        } else if (fallback != null) {
            try { fallback.run(); } catch (Exception ignored) { fragment.requireActivity().getOnBackPressedDispatcher().onBackPressed(); }
        } else {
            fragment.requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
    }
}
