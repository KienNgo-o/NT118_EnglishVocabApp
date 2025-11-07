package com.example.nt118_englishvocabapp.ui.quiz;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentQuizBinding;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

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

        try {
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

            // Standardized return behavior handled inline where needed (keyboard hide is called before navigation)

            // Make sure to return to home fragment properly
            if (binding.btnReturn != null) {
                binding.btnReturn.setOnClickListener(v -> {
                    keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(
                            requireActivity(), v, keyboardRootView, keyboardListener);

                    // prefer using MainActivity helper to keep BottomNavigationView state in sync
                    if (requireActivity() instanceof com.example.nt118_englishvocabapp.MainActivity) {
                        ((com.example.nt118_englishvocabapp.MainActivity) requireActivity()).navigateToHome();
                        return;
                    }

                    // fallback (should rarely run)
                    if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                        getParentFragmentManager().popBackStack();
                    } else {
                        AppCompatActivity activity = (AppCompatActivity) requireActivity();
                        activity.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.frame_layout, new com.example.nt118_englishvocabapp.ui.home.HomeFragment())
                                .commitAllowingStateLoss();
                    }
                });
            } else {
                Log.w("QuizFragment", "btnReturn view not found in binding");
            }

            // Populate the vertical zig-zag list of quiz stages
            try {
                setupStagesRecycler();
            } catch (Exception e) {
                // prevent crash and log stack trace so user can report it
                Log.e("QuizFragment", "Error setting up stages RecyclerView", e);
                Toast.makeText(getContext(), "Lỗi khi khởi tạo màn hình: " + e.getClass().getSimpleName() + ". Xem logcat để biết chi tiết.", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e("QuizFragment", "Unhandled error in onCreateView", e);
            writeStackTraceToFile(e);
            Toast.makeText(getContext(), "Lỗi nội bộ: " + e.getClass().getSimpleName() + ". Xem logcat.", Toast.LENGTH_LONG).show();
        }

        //final TextView textView = binding.textQuiz;
        //quizViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    private void writeStackTraceToFile(Exception e) {
        try {
            if (getContext() == null) return;
            File dir = getContext().getFilesDir();
            File out = new File(dir, "quiz_error.log");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String content = "[" + System.currentTimeMillis() + "]\n" + sw.toString() + "\n---\n";
            FileOutputStream fos = new FileOutputStream(out, true);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
        } catch (Exception ex) {
            Log.e("QuizFragment", "Failed to write crash log", ex);
        }
    }

    @SuppressLint("DiscouragedApi")
    private void setupStagesRecycler() {
        if (binding == null || getContext() == null) return;

        RecyclerView recyclerView = binding.stagesRecycler;
        GridLayoutManager glm = new GridLayoutManager(getContext(), 3);
        recyclerView.setLayoutManager(glm);
        recyclerView.setHasFixedSize(true);

        // add spacing between grid items so layout is less cramped
        final int spacingDp = 14; // reduced spacing to make parts slightly less large overall
        final int spacingPx = dpToPx(spacingDp);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view); // item position
                int spanCount = glm.getSpanCount();
                if (position == RecyclerView.NO_POSITION) return;
                int column = position % spanCount; // item column

                // Use a common grid spacing formula to distribute spacing evenly
                outRect.left = spacingPx - column * spacingPx / spanCount;
                outRect.right = (column + 1) * spacingPx / spanCount;
                outRect.top = spacingPx;
                outRect.bottom = spacingPx;
            }
        });

        List<StageItem> items = new ArrayList<>();
        final int totalStages = 12; // change as needed
        final int unlockedCount = 2; // demo: first two unlocked; replace with real progress logic

        // Zig-zag pattern columns sequence: 0,1,2,1 repeating
        int[] pattern = new int[]{0, 1, 2, 1};

        for (int i = 1; i <= totalStages; i++) {
            int targetCol = pattern[(i - 1) % pattern.length];
            for (int col = 0; col < 3; col++) {
                if (col == targetCol) {
                    boolean unlocked = i <= unlockedCount;
                    items.add(new StageItem(true, i, unlocked, "Stage " + i));
                } else {
                    items.add(new StageItem(false, 0, false, ""));
                }
            }
        }

        StageAdapter adapter = new StageAdapter(getContext(), items, this::onStageClick);
        recyclerView.setAdapter(adapter);
    }

    private void onStageClick(StageItem stageItem) {
        if (stageItem.isUnlocked()) {
            Toast.makeText(getContext(), "Open " + stageItem.getName(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), stageItem.getName() + " is locked", Toast.LENGTH_SHORT).show();
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
