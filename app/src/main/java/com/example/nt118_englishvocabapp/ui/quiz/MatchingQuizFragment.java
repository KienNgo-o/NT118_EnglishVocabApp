package com.example.nt118_englishvocabapp.ui.quiz;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.ui.home.HomeFragment;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;
import com.example.nt118_englishvocabapp.util.ReturnButtonHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchingQuizFragment extends Fragment {

    private View root;
    private View keyboardRootView;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;

    private GridLayout gridCards;
    private FrameLayout cardsContainer;
    private LineOverlay lineOverlay;
    private List<View> cardViews = new ArrayList<>();
    private int selectedIndex = -1; // index of first selected card
    private List<Pair<Integer, Integer>> connections = new ArrayList<>();
    private Map<Integer, Integer> matched = new HashMap<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_quiz_matching, container, false);

        // Keyboard visibility listener setup (same as other quiz fragments)
        if (getActivity() != null) {
            keyboardRootView = requireActivity().findViewById(android.R.id.content);
        } else {
            keyboardRootView = root;
        }

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

                if (isKeyboardVisible == lastStateVisible) return;
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

        // Return button behavior
        View.OnClickListener preClick = v -> {
            if (!isAdded()) return;
            keyboardListener = KeyboardUtils.hideKeyboardAndRestoreUI(requireActivity(), v, keyboardRootView, keyboardListener);
        };
        Runnable fallback = () -> {
            if (!isAdded()) return;
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new HomeFragment())
                    .commitAllowingStateLoss();
        };
        ReturnButtonHelper.bind(root, this, preClick, fallback);

        // Wire card grid and overlay
        gridCards = root.findViewById(R.id.grid_cards);
        cardsContainer = (FrameLayout) gridCards.getParent();

        // Collect card views dynamically (supports any even number of cards placed inside grid)
        for (int i = 0; i < gridCards.getChildCount(); i++) {
            View v = gridCards.getChildAt(i);
            if (v instanceof Button) {
                final int idx = cardViews.size();
                cardViews.add(v);
                v.setOnClickListener(view -> onCardClicked(idx));
            }
        }

        // Replace the placeholder overlay view with our custom LineOverlay so we can draw lines
        View placeholder = root.findViewById(R.id.line_overlay);
        lineOverlay = new LineOverlay(requireContext());
        // ensure layout params match parent
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        if (placeholder != null) {
            int index = cardsContainer.indexOfChild(placeholder);
            cardsContainer.removeView(placeholder);
            cardsContainer.addView(lineOverlay, index, lp);
        } else {
            cardsContainer.addView(lineOverlay, lp);
        }

        // Confirm button
        View btnConfirm = root.findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(v -> onConfirm());

        return root;
    }

    private void onCardClicked(int idx) {
        // ignore clicks on already matched cards
        if (matched.containsKey(idx) || matched.containsValue(idx)) return;

        View v = cardViews.get(idx);

        if (selectedIndex == -1) {
            // select first
            selectedIndex = idx;
            // visual: tint background to indicate selection
            v.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.correct_green));
            ((Button) v).setTextColor(Color.WHITE);
            // ensure overlay shows a temporary glow (optional)
        } else if (selectedIndex == idx) {
            // deselect
            v.setBackgroundResource(R.drawable.rounded_white);
            ((Button) v).setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_purple));
            selectedIndex = -1;
        } else {
            // second selection: create a connection between selectedIndex and idx
            int first = selectedIndex;
            int second = idx;
            connections.add(new Pair<>(first, second));
            matched.put(first, second);
            matched.put(second, first);

            // disable matched buttons to avoid reuse
            View v1 = cardViews.get(first);
            View v2 = cardViews.get(second);
            v1.setEnabled(false);
            v2.setEnabled(false);

            // reset visual selection for first (we will keep a colored state for matched)
            // set matched background to a semi-transparent green
            int green = ContextCompat.getColor(requireContext(), R.color.correct_green);
            v1.setBackgroundColor(green);
            ((Button) v1).setTextColor(Color.WHITE);
            v2.setBackgroundColor(green);
            ((Button) v2).setTextColor(Color.WHITE);

            // add line to overlay and redraw
            lineOverlay.addConnection(first, second);

            // clear selection state
            selectedIndex = -1;
        }
    }

    private void onConfirm() {
        // For now, just show a toast with number of connections
        String msg = "Connections: " + connections.size();
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();

        // TODO: validate correctness against expected answers and provide feedback
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (keyboardRootView != null && keyboardListener != null) {
            keyboardRootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
        }
        keyboardRootView = null;
        keyboardListener = null;
    }

    /* Inner view that draws lines between the centers of two card views */
    private class LineOverlay extends View {
        private final Paint paint;
        private final List<Pair<PointF, PointF>> lines = new ArrayList<>();

        public LineOverlay(@NonNull android.content.Context ctx) {
            super(ctx);
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(ContextCompat.getColor(ctx, R.color.correct_green));
            paint.setStrokeWidth(6f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        void addConnection(int idx1, int idx2) {
            if (idx1 < 0 || idx2 < 0 || idx1 >= cardViews.size() || idx2 >= cardViews.size()) return;

            // compute centers relative to this overlay
            PointF p1 = centerOfView(cardViews.get(idx1));
            PointF p2 = centerOfView(cardViews.get(idx2));
            if (p1 != null && p2 != null) {
                lines.add(new Pair<>(p1, p2));
                invalidate();
            }
        }

        private PointF centerOfView(View v) {
            if (!v.isShown()) return null;
            int[] loc = new int[2];
            int[] overlayLoc = new int[2];
            v.getLocationOnScreen(loc);
            this.getLocationOnScreen(overlayLoc);
            float x = loc[0] - overlayLoc[0] + v.getWidth() / 2f;
            float y = loc[1] - overlayLoc[1] + v.getHeight() / 2f;
            return new PointF(x, y);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            for (Pair<PointF, PointF> p : lines) {
                canvas.drawLine(p.first.x, p.first.y, p.second.x, p.second.y, paint);
            }
        }
    }
}
