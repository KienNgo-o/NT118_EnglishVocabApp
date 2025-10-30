package com.example.nt118_englishvocabapp.ui.quiz;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentQuizBinding;
import com.example.nt118_englishvocabapp.ui.home.HomeFragment;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;
import com.example.nt118_englishvocabapp.util.ReturnButtonHelper;

public class QuizFragment extends Fragment {

    private FragmentQuizBinding binding;
    private View keyboardRootView;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // ViewModel is not currently used in this fragment; create it later if needed
        // QuizViewModel quizViewModel = new ViewModelProvider(this).get(QuizViewModel.class);

        binding = FragmentQuizBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Optional toast to match other fragments' behavior
        Toast.makeText(getContext(), "Quiz Fragment Opened!", Toast.LENGTH_SHORT).show();

        // Use Activity content view as stable root for keyboard detection
        if (getActivity() != null) {
            keyboardRootView = requireActivity().findViewById(android.R.id.content);
        } else {
            keyboardRootView = root; // fallback
        }

        // Keyboard visibility listener: hide bottom menu and FAB when keyboard is shown
        keyboardListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private boolean lastStateVisible = false;

            @Override
            public void onGlobalLayout() {
                if (keyboardRootView == null || getActivity() == null) return;

                Rect r = new Rect();
                keyboardRootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = keyboardRootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                boolean isKeyboardVisible = keypadHeight > screenHeight * 0.15;

                if (isKeyboardVisible == lastStateVisible) return; // no change
                lastStateVisible = isKeyboardVisible;

                View bottomAppBar = requireActivity().findViewById(R.id.bottomAppBar);
                View fab = requireActivity().findViewById(R.id.fab);

                if (bottomAppBar != null) bottomAppBar.setVisibility(isKeyboardVisible ? View.GONE : View.VISIBLE);
                if (fab != null) fab.setVisibility(isKeyboardVisible ? View.GONE : View.VISIBLE);
            }
        };

        if (keyboardRootView != null) {
            keyboardRootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
        }

        // Standardized return behavior: hide keyboard first, then pop backstack or navigate home as a fallback
        View.OnClickListener preClick = v -> {
            if (!isAdded()) return;
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                    requireActivity(),
                    v,
                    keyboardRootView,
                    keyboardListener
            );
        };

        Runnable fallback = () -> {
            if (!isAdded()) return;
            AppCompatActivity activity = (AppCompatActivity) requireActivity();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new HomeFragment())
                    .commitAllowingStateLoss();
        };

        ReturnButtonHelper.bind(binding.getRoot(), this, preClick, fallback);

        // Populate the vertical zig-zag list of quiz stages
        setupStages();

        //final TextView textView = binding.textQuiz;
        //quizViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @SuppressLint("DiscouragedApi")
    private void setupStages() {
        if (binding == null || getContext() == null) return;

        LinearLayout container = binding.stagesContainer;
        container.removeAllViews();

        final int totalStages = 12; // change as needed
        final int unlockedCount = 2; // demo: first two unlocked; replace with real progress logic

        int imageSize = dpToPx(72);
        // Increase horizontalOffset so both left and right items are pushed inward toward center
        int horizontalOffset = dpToPx(80); // was 24dp; increase to move items closer to center
        int verticalSpacing = dpToPx(20);

        for (int i = 0; i < totalStages; i++) {
            final int stageIndex = i + 1;
            boolean unlocked = stageIndex <= unlockedCount;

            // Row that fills width
            FrameLayout row = new FrameLayout(getContext());
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.topMargin = verticalSpacing / 2;
            rowLp.bottomMargin = verticalSpacing / 2;
            row.setLayoutParams(rowLp);

            // The stage view (image + label) -- same as before
            LinearLayout stageCard = new LinearLayout(getContext());
            stageCard.setOrientation(LinearLayout.VERTICAL);
            stageCard.setGravity(Gravity.CENTER_HORIZONTAL);

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            stageCard.setLayoutParams(cardLp);

            // Build a circular base and a separate image on top so the image is NOT the background.
            FrameLayout imageFrame = new FrameLayout(getContext());
            LinearLayout.LayoutParams imageFrameLp = new LinearLayout.LayoutParams(imageSize, imageSize);
            imageFrameLp.gravity = Gravity.CENTER_HORIZONTAL;
            imageFrame.setLayoutParams(imageFrameLp);

            // Circular base view (background circle)
            View baseCircle = new View(getContext());
            FrameLayout.LayoutParams baseLp = new FrameLayout.LayoutParams(imageSize, imageSize, Gravity.CENTER);
            baseCircle.setLayoutParams(baseLp);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            int bgColor = unlocked ? ContextCompat.getColor(getContext(), R.color.light_purple) : ContextCompat.getColor(getContext(), android.R.color.darker_gray);
            bg.setColor(bgColor);
            bg.setStroke(dpToPx(2), ContextCompat.getColor(getContext(), R.color.dark_purple));
            baseCircle.setBackground(bg);
            imageFrame.addView(baseCircle);

            // Top image (shows the topic picture) - smaller than base so the circular rim remains visible
            int innerPadding = dpToPx(14); // adjust if you want a bigger/smaller image inside the circle
            int innerSize = imageSize - innerPadding * 2;
            ImageView imgTop = new ImageView(getContext());
            FrameLayout.LayoutParams imgLp = new FrameLayout.LayoutParams(innerSize, innerSize, Gravity.CENTER);
            imgTop.setLayoutParams(imgLp);
            imgTop.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            // Explicit mapping requested by user: stage 1 -> "fruits", 2 -> "animals", 3 -> "careers".
            // Drawables should be placed by you in res/drawable with those names (e.g. fruits.png)
            String imageName;
            switch (stageIndex) {
                case 1:
                    imageName = "fruits";
                    break;
                case 2:
                    imageName = "animals";
                    break;
                case 3:
                    imageName = "careers";
                    break;
                default:
                    // fallback convention: try stage_N if you later want to name them that way
                    imageName = "stage_" + stageIndex;
                    break;
            }

            int drawableId = getResources().getIdentifier(imageName, "drawable", getContext().getPackageName());
            if (drawableId != 0) {
                imgTop.setImageResource(drawableId);
            } else {
                // fallback placeholder while you add the real drawables
                imgTop.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            imgTop.setContentDescription(getString(R.string.part_label, stageIndex));
            imageFrame.addView(imgTop);

            // Click behavior attached to the frame so tapping anywhere on the circular button triggers it
            imageFrame.setClickable(true);
            imageFrame.setOnClickListener(v -> {
                if (unlocked) {
                    Toast.makeText(getContext(), "Open Stage " + stageIndex, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Stage " + stageIndex + " is locked", Toast.LENGTH_SHORT).show();
                }
            });

            // Lock overlay if needed
            if (!unlocked) {
                ImageView lockIv = new ImageView(getContext());
                int lockSize = dpToPx(20);
                FrameLayout.LayoutParams lockLp = new FrameLayout.LayoutParams(lockSize, lockSize, Gravity.END | Gravity.BOTTOM);
                int lockMargin = dpToPx(4);
                lockLp.setMargins(0, 0, lockMargin, lockMargin);
                lockIv.setLayoutParams(lockLp);
                // Use framework lock icon as a fallback; you can replace with your drawable in res/drawable
                lockIv.setImageResource(android.R.drawable.ic_lock_lock);
                lockIv.setColorFilter(ContextCompat.getColor(getContext(), android.R.color.black));
                imageFrame.addView(lockIv);
            }

            stageCard.addView(imageFrame);

            TextView label = new TextView(getContext());
            label.setText(getString(R.string.part_label, stageIndex));
            label.setTextColor(ContextCompat.getColor(getContext(), R.color.dark_purple));
            label.setTextSize(14f);
            label.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lblParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lblParams.topMargin = dpToPx(6);
            label.setLayoutParams(lblParams);
            stageCard.addView(label);

            // Alternate gravity left / right for zig-zag
            boolean alignLeft = (i % 2 == 0);
            FrameLayout.LayoutParams stageLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            stageLp.gravity = alignLeft ? Gravity.START | Gravity.CENTER_VERTICAL : Gravity.END | Gravity.CENTER_VERTICAL;

            // Use symmetric horizontal margins so both start/end items are equally offset toward the center
            stageLp.setMargins(horizontalOffset, 0, horizontalOffset, 0);

            row.addView(stageCard, stageLp);

            container.addView(row);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listener from the same view it was added to
        if (keyboardRootView != null && keyboardListener != null) {
            keyboardRootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
        }
        binding = null;
        keyboardRootView = null;
        keyboardListener = null;
    }
}
