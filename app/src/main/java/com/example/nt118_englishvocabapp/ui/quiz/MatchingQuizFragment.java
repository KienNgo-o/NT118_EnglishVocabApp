// java
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
import androidx.cardview.widget.CardView;
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
    private int selectedIndex = -1;
    private List<Pair<Integer, Integer>> connections = new ArrayList<>();
    private Map<Integer, Integer> matched = new HashMap<>();
    private boolean isConfirmed = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_quiz_matching, container, false);

        // keyboard listener (unchanged)
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

        // Return button
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

        // Wire grid and overlay
        gridCards = root.findViewById(R.id.grid_cards);
        cardsContainer = (FrameLayout) gridCards.getParent();

        // Collect card views: support both Button and CardView (image) tiles
        cardViews.clear();
        for (int i = 0; i < gridCards.getChildCount(); i++) {
            View v = gridCards.getChildAt(i);
            // Add only top-level tiles (Button or CardView)
            if (v instanceof Button || v instanceof CardView) {
                final int idx = cardViews.size();
                cardViews.add(v);
                v.setOnClickListener(view -> onCardClicked(idx));

                // Default visuals: white background and dark gray text for choice buttons
                v.setBackgroundResource(R.drawable.rounded_white);
                setTextColorIfButton(v, R.color.white);

                v.setEnabled(true);
            }
        }

        // Replace overlay placeholder with LineOverlay
        View placeholder = root.findViewById(R.id.line_overlay);
        lineOverlay = new LineOverlay(requireContext());
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
        if (isConfirmed) return;

        // if already matched, remove match on tap
        if (matched.containsKey(idx)) {
            Integer partnerObj = matched.get(idx);
            if (partnerObj != null) {
                int partner = partnerObj;
                removeConnectionBetween(partner, idx);
                View v1 = cardViews.get(partner);
                View v2 = cardViews.get(idx);
                v1.setEnabled(true);
                v2.setEnabled(true);
                v1.setBackgroundResource(R.drawable.rounded_white);
                v2.setBackgroundResource(R.drawable.rounded_white);
                setTextColorIfButton(v1, R.color.dark_purple);
                setTextColorIfButton(v2, R.color.dark_purple);
            }
            selectedIndex = -1;
            return;
        }

        View v = cardViews.get(idx);

        if (selectedIndex == -1) {
            // select first
            selectedIndex = idx;
            v.setBackgroundResource(R.drawable.rounded_orange);
            setTextColorIfButton(v, R.color.white);
        } else if (selectedIndex == idx) {
            // unselect
            v.setBackgroundResource(R.drawable.rounded_white);
            setTextColorIfButton(v, R.color.dark_purple);
            selectedIndex = -1;
        } else {
            // second selection -> attempt match
            int first = selectedIndex;
            int colFirst = columnOfCard(first);
            int colSecond = columnOfCard(idx);
            if (colFirst != -1 && colSecond != -1 && colFirst == colSecond) {
                Toast.makeText(requireContext(), "Cannot match two cards in the same column", Toast.LENGTH_SHORT).show();
                return;
            }

            // create connection and mark as matched
            connections.add(new Pair<>(first, idx));
            matched.put(first, idx);
            matched.put(idx, first);

            View v1 = cardViews.get(first);
            View v2 = cardViews.get(idx);

            // disable matched tiles to avoid rematching unless user taps again to remove
            v1.setEnabled(false);
            v2.setEnabled(false);

            v1.setBackgroundResource(R.drawable.rounded_orange);
            v2.setBackgroundResource(R.drawable.rounded_orange);
            setTextColorIfButton(v1, R.color.white);
            setTextColorIfButton(v2, R.color.white);

            lineOverlay.addConnection(first, idx);
            selectedIndex = -1;
        }
    }

    private void setTextColorIfButton(View v, int colorRes) {
        if (v instanceof Button) {
            ((Button) v).setTextColor(ContextCompat.getColor(requireContext(), colorRes));
        }
    }

    private int columnOfCard(int idx) {
        if (idx < 0 || idx >= cardViews.size()) return -1;
        View v = cardViews.get(idx);
        if (!v.isShown()) return -1;
        int[] gridLoc = new int[2];
        gridCards.getLocationOnScreen(gridLoc);
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        float centerX = (loc[0] - gridLoc[0]) + v.getWidth() / 2f;
        int gridWidth = gridCards.getWidth();
        if (gridWidth <= 0) return -1;
        return (centerX < gridWidth / 2f) ? 0 : 1;
    }

    private void removeConnectionBetween(int a, int b) {
        for (int i = connections.size() - 1; i >= 0; i--) {
            Pair<Integer, Integer> p = connections.get(i);
            if ((p.first == a && p.second == b) || (p.first == b && p.second == a)) {
                connections.remove(i);
            }
        }
        matched.remove(a);
        matched.remove(b);
        lineOverlay.removeConnection(a, b);
    }

    private void onConfirm() {
        if (isConfirmed) return;
        isConfirmed = true;

        for (Pair<Integer, Integer> p : new ArrayList<>(connections)) {
            int a = p.first;
            int b = p.second;
            if (a < 0 || b < 0 || a >= cardViews.size() || b >= cardViews.size()) continue;
            View va = cardViews.get(a);
            View vb = cardViews.get(b);
            String tagA = (va.getTag() != null) ? va.getTag().toString().trim().toLowerCase() : null;
            String tagB = (vb.getTag() != null) ? vb.getTag().toString().trim().toLowerCase() : null;
            boolean correct = tagA != null && tagA.equals(tagB);

            if (correct) {
                va.setBackgroundResource(R.drawable.rounded_green);
                vb.setBackgroundResource(R.drawable.rounded_green);
                setTextColorIfButton(va, R.color.white);
                setTextColorIfButton(vb, R.color.white);
            } else {
                va.setBackgroundResource(R.drawable.rounded_red);
                vb.setBackgroundResource(R.drawable.rounded_red);
                setTextColorIfButton(va, R.color.white);
                setTextColorIfButton(vb, R.color.white);
            }
        }

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

    /* LineOverlay inner class (unchanged) */
    private class LineOverlay extends View {
        private final Paint paint;
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
