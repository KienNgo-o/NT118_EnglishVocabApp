package com.example.nt118_englishvocabapp.ui.quiz;

import android.graphics.Canvas;
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
    private boolean isConfirmed = false; // when true, lock UI and show final colors

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
                // ensure default visuals are the rounded white drawable
                v.setBackgroundResource(R.drawable.rounded_white);
                ((Button) v).setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_purple));
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
        if (isConfirmed) return; // lock interactions after confirm

        // If this card is already matched, tapping it should undo the match (allow rechoice)
        if (matched.containsKey(idx)) {
            Integer partnerObj = matched.get(idx);
            if (partnerObj != null) {
                int partner = partnerObj;
                removeConnectionBetween(partner, idx);
                // reset visuals and re-enable
                View v1 = cardViews.get(partner);
                View v2 = cardViews.get(idx);
                v1.setEnabled(true);
                v2.setEnabled(true);
                v1.setBackgroundResource(R.drawable.rounded_white);
                v2.setBackgroundResource(R.drawable.rounded_white);
                ((Button) v1).setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_purple));
                ((Button) v2).setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_purple));
                Toast.makeText(requireContext(), "Match removed", Toast.LENGTH_SHORT).show();
            }
            selectedIndex = -1;
            return;
        }

        View v = cardViews.get(idx);

        if (selectedIndex == -1) {
            // select first - use orange drawable
            selectedIndex = idx;
            v.setBackgroundResource(R.drawable.rounded_orange);
            ((Button) v).setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else if (selectedIndex == idx) {
            // deselect
            v.setBackgroundResource(R.drawable.rounded_white);
            ((Button) v).setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_purple));
            selectedIndex = -1;
        } else {
            // second selection: before creating connection, ensure not same column
            int first = selectedIndex;
            // int second = idx; // redundant
            int colFirst = columnOfCard(first);
            int colSecond = columnOfCard(idx);
            // if we can't determine column for either (returned -1), allow the match; otherwise disallow same-column matches
            if (colFirst != -1 && colSecond != -1 && colFirst == colSecond) {
                // same column - reject
                Toast.makeText(requireContext(), "Cannot match two cards in the same column", Toast.LENGTH_SHORT).show();
                // keep the first selected so user can pick a correct pair
                return;
            }

            // create connection between first and second
            connections.add(new Pair<>(first, idx));
            matched.put(first, idx);
            matched.put(idx, first);

            // mark matched visuals with orange (still considered matched but not yet graded)
            View v1 = cardViews.get(first);
            View v2 = cardViews.get(idx);
            // Keep matched buttons enabled/clickable so user can tap to undo the match and rechoose
            v1.setEnabled(true);
            v2.setEnabled(true);
            v1.setBackgroundResource(R.drawable.rounded_orange);
            v2.setBackgroundResource(R.drawable.rounded_orange);
            ((Button) v1).setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            ((Button) v2).setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

            // add line to overlay and redraw
            lineOverlay.addConnection(first, idx);

            // clear selection state
            selectedIndex = -1;
        }
    }

    /** Return column index (0-based) for a card, or -1 if cannot determine. Use card center relative to grid width. */
    private int columnOfCard(int idx) {
        if (idx < 0 || idx >= cardViews.size()) return -1;
        View v = cardViews.get(idx);
        if (!v.isShown()) return -1;
        int[] gridLoc = new int[2];
        gridCards.getLocationOnScreen(gridLoc);
        float gridLeft = gridLoc[0];
        float gridWidth = gridCards.getWidth();
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        float centerX = loc[0] - gridLeft + v.getWidth() / 2f;
        if (gridWidth <= 0) return -1;
        return (centerX < gridWidth / 2f) ? 0 : 1;
    }

    private void removeConnectionBetween(int a, int b) {
        // remove from connections list (order-insensitive)
        for (int i = connections.size() - 1; i >= 0; i--) {
            Pair<Integer, Integer> p = connections.get(i);
            if ((p.first == a && p.second == b) || (p.first == b && p.second == a)) {
                connections.remove(i);
            }
        }
        // remove matched mapping both ways
        matched.remove(a);
        matched.remove(b);
        // request overlay to remove visual line
        lineOverlay.removeConnection(a, b);
    }

    private void onConfirm() {
        if (isConfirmed) return;
        isConfirmed = true;

        // evaluate each connection: compare tags on the two buttons
        for (Pair<Integer, Integer> p : new ArrayList<>(connections)) {
            int a = p.first;
            int b = p.second;
            if (a < 0 || b < 0 || a >= cardViews.size() || b >= cardViews.size()) continue;
            View va = cardViews.get(a);
            View vb = cardViews.get(b);
            String tagA = (va.getTag() != null) ? va.getTag().toString() : null;
            String tagB = (vb.getTag() != null) ? vb.getTag().toString() : null;
            boolean correct = tagA != null && tagA.equals(tagB);

            if (correct) {
                // green for correct - use drawable so stroke/corner consistent
                va.setBackgroundResource(R.drawable.rounded_green);
                vb.setBackgroundResource(R.drawable.rounded_green);
                ((Button) va).setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                ((Button) vb).setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            } else {
                // red for wrong - use drawable
                va.setBackgroundResource(R.drawable.rounded_red);
                vb.setBackgroundResource(R.drawable.rounded_red);
                ((Button) va).setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                ((Button) vb).setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            }
        }

        // After grading, lock all card interactions
        for (View c : cardViews) {
            c.setEnabled(false);
        }

        Toast.makeText(requireContext(), "Answers checked", Toast.LENGTH_SHORT).show();
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
        // store pairs of indices rather than precomputed points so we can recalc on layout changes
        private final List<Pair<Integer, Integer>> indexPairs = new ArrayList<>();

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
            // avoid duplicates
            for (Pair<Integer, Integer> p : indexPairs) {
                if ((p.first == idx1 && p.second == idx2) || (p.first == idx2 && p.second == idx1)) return;
            }
            indexPairs.add(new Pair<>(idx1, idx2));
            invalidate();
        }

        void removeConnection(int idx1, int idx2) {
            for (int i = indexPairs.size() - 1; i >= 0; i--) {
                Pair<Integer, Integer> p = indexPairs.get(i);
                if ((p.first == idx1 && p.second == idx2) || (p.first == idx2 && p.second == idx1)) {
                    indexPairs.remove(i);
                }
            }
            invalidate();
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
            // draw lines by computing centers now (handles layout changes)
            for (Pair<Integer, Integer> p : indexPairs) {
                int a = p.first;
                int b = p.second;
                if (a < 0 || b < 0 || a >= cardViews.size() || b >= cardViews.size()) continue;
                PointF p1 = centerOfView(cardViews.get(a));
                PointF p2 = centerOfView(cardViews.get(b));
                if (p1 != null && p2 != null) {
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint);
                }
            }
        }
    }
}
