package com.example.nt118_englishvocabapp.ui.quiz;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.R;

import java.util.ArrayList;
import java.util.List;

public class ChooseAnswerQuizFragment extends Fragment {

    private FrameLayout answerArea;
    private MediaPlayer mediaPlayer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Single-question model: multiple choices where each round plays a sound for one remaining choice
    static class Question {
        Uri soundUri; // unused in demo, could be per-question
        String correctAnswerId; // id of the current target choice
        List<AnswerItem> choices; // remaining choices on screen

        Question(Uri soundUri, String correctAnswerId, List<AnswerItem> choices) {
            this.soundUri = soundUri;
            this.correctAnswerId = correctAnswerId;
            this.choices = choices;
        }
    }

    static class AnswerItem {
        String id; // unique id
        String text; // display text (kept as metadata even for image tiles)
        Uri imageUri; // optional image URI
        Integer imageResId; // optional drawable resource id to display as image

        AnswerItem(String id, String text, Uri imageUri, Integer imageResId) {
            this.id = id;
            this.text = text;
            this.imageUri = imageUri;
            this.imageResId = imageResId;
        }
    }

    private Question currentQuestion;
    private List<String> pendingWords = null; // store backend words if set before view creation

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_chooseanswer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        answerArea = view.findViewById(R.id.answer_area);
        ImageButton btnPlay = view.findViewById(R.id.btn_play_sound);

        // If backend provided words earlier, apply them; otherwise use demo sample words
        if (pendingWords != null) {
            applyWords(pendingWords);
            pendingWords = null;
        } else {
            // Demo sample list: you can remove/replace this and call setWords(...) from the activity
            List<String> sample = new ArrayList<>();
            sample.add("pen");
            sample.add("apple");
            sample.add("banana");
            sample.add("mango");
            applyWords(sample);
        }

        btnPlay.setOnClickListener(v -> playCurrentQuestionSound());
    }

    /**
     * Public API: call this from your activity/fragment coordinator when the backend provides words.
     * Expected: a list with at least 2 items; we will take up to 4 words and present exactly 4 tiles
     * (if fewer than 4 words are given we'll duplicate/pad with placeholders).
     */
    public void setWords(List<String> words) {
        if (words == null) return;
        List<String> copy = new ArrayList<>(words);
        if (getView() == null) {
            pendingWords = copy;
        } else {
            applyWords(copy);
        }
    }

    // Build exactly four choices from the provided words: 2 text tiles and 2 image tiles (prefer matching drawables)
    private void applyWords(List<String> words) {
        // Normalize input and ensure at least 4 entries by repeating last item if necessary
        List<String> w = new ArrayList<>();
        for (String s : words) if (s != null && !s.trim().isEmpty()) w.add(s.trim());
        while (w.size() < 4) {
            if (w.isEmpty()) w.add("item"); else w.add(w.get(w.size() - 1));
        }

        // We'll take the first 4 words and detect which have drawables
        int placeholder = R.drawable.banana; // fallback image when word drawable not found
        int[] foundRid = new int[4]; // 0 means none; otherwise resource id
        for (int i = 0; i < 4; i++) {
            String word = w.get(i);
            String resName = word.toLowerCase().replaceAll("[^a-z0-9_]+", "_");
            int rid = 0;
            try {
                rid = getResources().getIdentifier(resName, "drawable", requireContext().getPackageName());
            } catch (Exception ignored) {}
            foundRid[i] = rid; // may be 0
        }

        // Collect indices that have matching drawables
        List<Integer> drawableIndices = new ArrayList<>();
        for (int i = 0; i < 4; i++) if (foundRid[i] != 0) drawableIndices.add(i);

        // We'll choose exactly two indices for image tiles.
        int[] imageIndices = new int[2];
        if (drawableIndices.size() >= 2) {
            imageIndices[0] = drawableIndices.get(0);
            imageIndices[1] = drawableIndices.get(1);
        } else if (drawableIndices.size() == 1) {
            imageIndices[0] = drawableIndices.get(0);
            // pick another index that's not the same
            imageIndices[1] = (imageIndices[0] == 0) ? 1 : 0;
        } else {
            // no matching drawables; pick positions 1 and 3 as default image slots
            imageIndices[0] = 1;
            imageIndices[1] = 3;
        }

        // Ensure imageIndices are distinct
        if (imageIndices[0] == imageIndices[1]) {
            imageIndices[1] = (imageIndices[0] + 1) % 4;
        }

        // Build choices: if an index is selected for image, use foundRid if available, else placeholder
        List<AnswerItem> choices = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String word = w.get(i);
            String id = "choice_" + i;
            boolean isImage = (i == imageIndices[0] || i == imageIndices[1]);
            if (isImage) {
                int rid = foundRid[i] != 0 ? foundRid[i] : placeholder;
                choices.add(new AnswerItem(id, word, null, rid));
            } else {
                choices.add(new AnswerItem(id, word, null, null));
            }
        }

        // Determine initial correct answer id: prefer first text tile; fallback to first item
        String initialCorrectId = null;
        for (AnswerItem ai : choices) {
            if (ai.imageResId == null) { initialCorrectId = ai.id; break; }
        }
        if (initialCorrectId == null && !choices.isEmpty()) initialCorrectId = choices.get(0).id;

        currentQuestion = new Question(null, initialCorrectId, new ArrayList<>(choices));

        showQuestion();
    }

    // Show all remaining tiles (currentQuestion.choices) on the answerArea
    private void showQuestion() {
        answerArea.removeAllViews();
        if (currentQuestion == null || currentQuestion.choices == null) return;

        // tile sizes
        int tileWdp = 120;
        int tileHdp = 56;
        int tileW = dpToPx(tileWdp);
        int tileH = dpToPx(tileHdp);

        answerArea.post(() -> {
            int areaW = answerArea.getWidth();
            int areaH = answerArea.getHeight();
            if (areaW == 0 || areaH == 0) {
                areaW = dpToPx(300);
                areaH = dpToPx(400);
            }

            // Keep choices order stable (no shuffle) and place them in a deterministic grid
            int n = currentQuestion.choices.size();
            int cols = Math.min(2, Math.max(1, n)); // prefer 2 columns if multiple items
            int rows = (n + cols - 1) / cols;

            int cellW = areaW / cols;
            int cellH = areaH / Math.max(1, rows);

            for (int i = 0; i < n; i++) {
                AnswerItem ai = currentQuestion.choices.get(i);

                CardView card = new CardView(requireContext());
                // If this choice is an image tile, make the card square (width x width).
                int cardH = (ai.imageResId != null || ai.imageUri != null) ? tileW : tileH;
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(tileW, cardH);

                int col = i % cols;
                int row = i / cols;

                int x = col * cellW + Math.max(0, (cellW - tileW) / 2);
                // Vertical centering should use the actual card height
                int y = row * cellH + Math.max(0, (cellH - cardH) / 2);
                lp.leftMargin = x;
                lp.topMargin = y;

                card.setLayoutParams(lp);
                card.setCardElevation(dpToPx(2));
                card.setRadius(dpToPx(8));
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white));

                if (ai.imageResId != null || ai.imageUri != null) {
                    ImageView iv = new ImageView(requireContext());
                    // Fill the square card fully
                    iv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                     iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                     try {
                         if (ai.imageResId != null) {
                             iv.setImageResource(ai.imageResId);
                         } else {
                             iv.setImageURI(ai.imageUri);
                         }
                         card.addView(iv);
                     } catch (Exception e) {
                         // fallback to text
                         TextView tv = buildTileText(ai.text);
                         card.addView(tv);
                     }
                 } else {
                     TextView tv = buildTileText(ai.text);
                     card.addView(tv);
                 }

                card.setTag(ai.id);
                card.setOnClickListener(v -> handleAnswerSelection(ai.id, card));
                answerArea.addView(card);
            }
        });
    }

    private TextView buildTileText(String text) {
        TextView tv = new TextView(requireContext());
        tv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tv.setText(text != null ? text : "(img)");
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_purple));
        tv.setTextSize(16);
        tv.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setGravity(android.view.Gravity.CENTER);
        return tv;
    }

    // Handle selection: if correct, remove the tile; if incorrect, flash red
    private void handleAnswerSelection(String answerId, CardView selectedCard) {
        if (currentQuestion == null) return;

        AnswerItem chosen = null;
        for (AnswerItem ai : currentQuestion.choices) {
            if (ai.id.equals(answerId)) {
                chosen = ai; break;
            }
        }
        if (chosen == null) return;

        boolean correct = chosen.id.equals(currentQuestion.correctAnswerId);
        if (correct) {
            // remove the tapped view from layout and the item from the model (no re-layout)
            answerArea.removeView(selectedCard);
            currentQuestion.choices.remove(chosen);
            Toast.makeText(getContext(), "Correct!", Toast.LENGTH_SHORT).show();

            // If there are remaining choices, pick the next target (prefer a text tile)
            if (!currentQuestion.choices.isEmpty()) {
                String nextId = null;
                for (AnswerItem ai : currentQuestion.choices) {
                    if (ai.imageResId == null) { nextId = ai.id; break; }
                }
                if (nextId == null) nextId = currentQuestion.choices.get(0).id;
                currentQuestion.correctAnswerId = nextId;

                // Do not rearrange other tiles — keep their existing positions and views.
                // Auto-play the next sound (demo behavior: shows a Toast if no real Uri)
                mainHandler.postDelayed(this::playCurrentQuestionSound, 600);

            } else {
                // nothing left: show finished message; UI already has the tapped card removed
                Toast.makeText(getContext(), "Quiz finished!", Toast.LENGTH_LONG).show();
            }

        } else {
            int originalColor = ContextCompat.getColor(requireContext(), R.color.white);
            selectedCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.incorrect_red));
            mainHandler.postDelayed(() -> selectedCard.setCardBackgroundColor(originalColor), 800);
            Toast.makeText(getContext(), "Try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void playCurrentQuestionSound() {
        if (currentQuestion == null) {
            Toast.makeText(getContext(), "No question", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find the current target item by id
        AnswerItem target = null;
        for (AnswerItem ai : currentQuestion.choices) {
            if (ai.id.equals(currentQuestion.correctAnswerId)) { target = ai; break; }
        }
        // If not found among remaining (e.g., just removed), try to find in previous choices by updating logic may have set id to null
        if (target == null) {
            // try to pick any remaining item
            if (!currentQuestion.choices.isEmpty()) target = currentQuestion.choices.get(0);
        }

        // In demo we don't have per-item audio URIs; show a Toast with the word
        if (target != null) {
            Toast.makeText(getContext(), "(Demo) Play sound for: " + target.text, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Cannot play sound", Toast.LENGTH_SHORT).show();
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (IllegalStateException ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onStop() {
        super.onStop();
        releasePlayer();
    }
}
