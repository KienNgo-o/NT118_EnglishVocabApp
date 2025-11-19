package com.example.nt118_englishvocabapp.ui.quiz;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.GridLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.R;

import java.util.ArrayList;
import java.util.List;

public class ChooseAudioAnswerQuizFragment extends Fragment {

    private GridLayout answerArea; // changed from FrameLayout to GridLayout
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
        return inflater.inflate(R.layout.fragment_quiz_chooseaudioanswer, container, false);
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

    private void applyWords(List<String> words) {
        // Normalize input and ensure at least 4 entries
        List<String> w = new ArrayList<>();
        for (String s : words) if (s != null && !s.trim().isEmpty()) w.add(s.trim());
        while (w.size() < 4) {
            if (w.isEmpty()) w.add("item"); else w.add(w.get(w.size() - 1));
        }

        int placeholder = R.drawable.banana; // fallback image when word drawable not found

        // Build choices: all 4 will be images
        List<AnswerItem> choices = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String word = w.get(i);
            String id = "choice_" + i;
            String resName = word.toLowerCase().replaceAll("[^a-z0-9_]+", "_");
            int rid = 0;
            try {
                rid = getResources().getIdentifier(resName, "drawable", requireContext().getPackageName());
            } catch (Exception ignored) {}

            // Use found drawable or placeholder
            int finalRid = rid != 0 ? rid : placeholder;
            choices.add(new AnswerItem(id, word, null, finalRid));
        }

        // Pick first choice as initial correct answer
        String initialCorrectId = !choices.isEmpty() ? choices.get(0).id : null;
        currentQuestion = new Question(null, initialCorrectId, new ArrayList<>(choices));

        showQuestion();
    }

    private void showQuestion() {
        View view = getView();
        if (view == null || currentQuestion == null || currentQuestion.choices == null) return;

        CardView[] cards = new CardView[]{
                view.findViewById(R.id.card_option_1),
                view.findViewById(R.id.card_option_2),
                view.findViewById(R.id.card_option_3),
                view.findViewById(R.id.card_option_4)
        };

        ImageView[] images = new ImageView[]{
                view.findViewById(R.id.img_option_1),
                view.findViewById(R.id.img_option_2),
                view.findViewById(R.id.img_option_3),
                view.findViewById(R.id.img_option_4)
        };

        // Populate the 4 tiles
        for (int i = 0; i < 4 && i < currentQuestion.choices.size(); i++) {
            AnswerItem ai = currentQuestion.choices.get(i);
            final int index = i;

            if (ai.imageResId != null) {
                images[index].setImageResource(ai.imageResId);
            } else if (ai.imageUri != null) {
                images[index].setImageURI(ai.imageUri);
            }
            images[index].setAdjustViewBounds(true);
            images[index].setScaleType(ImageView.ScaleType.FIT_CENTER);
            int pad = dpToPx(8);
            images[index].setPadding(pad, pad, pad, pad);
            cards[index].setTag(ai.id);
            cards[index].setVisibility(View.VISIBLE);
            cards[index].setOnClickListener(v -> handleAnswerSelection(ai.id, cards[index]));
        }

        // Hide unused cards if less than 4 choices remain
        for (int i = currentQuestion.choices.size(); i < 4; i++) {
            cards[i].setVisibility(View.INVISIBLE);
        }
    }

    private TextView buildTileText(String text) {
        TextView tv = new TextView(requireContext());
        tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tv.setText(text != null ? text : "(img)");
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_purple));
        tv.setTextSize(16);
        tv.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setGravity(android.view.Gravity.CENTER);
        return tv;
    }

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
            // Hide the card instead of removing
            selectedCard.setVisibility(View.INVISIBLE);
            currentQuestion.choices.remove(chosen);
            Toast.makeText(getContext(), "Correct!", Toast.LENGTH_SHORT).show();

            if (!currentQuestion.choices.isEmpty()) {
                // Pick next target (first remaining choice)
                currentQuestion.correctAnswerId = currentQuestion.choices.get(0).id;
                mainHandler.postDelayed(this::playCurrentQuestionSound, 600);
            } else {
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
