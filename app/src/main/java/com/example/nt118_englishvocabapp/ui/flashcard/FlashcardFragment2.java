package com.example.nt118_englishvocabapp.ui.flashcard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentFlashcard2Binding;
import com.example.nt118_englishvocabapp.models.Definition;
import com.example.nt118_englishvocabapp.models.FlashcardItem;
import com.example.nt118_englishvocabapp.models.LearnableItem;
import com.example.nt118_englishvocabapp.models.Pronunciation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FlashcardFragment2 extends Fragment {

    private static final String TAG = "FlashcardFragment2";
    private FragmentFlashcard2Binding binding;
    private int topicId = 1;
    private List<LearnableItem> learnableItems = new ArrayList<>();
    private int currentIndex = 0;
    private boolean showingFront = true;
    private boolean isFlipping = false;
    private int lastIndex = -1;

    private FlashcardViewModel viewModel;
    private MediaPlayer mediaPlayer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFlashcard2Binding.inflate(inflater, container, false);
        View root = binding.getRoot();

        viewModel = new ViewModelProvider(requireActivity()).get(FlashcardViewModel.class);
        binding.backView.setRotationY(180f);

        if (getArguments() != null) {
            topicId = getArguments().getInt("topic_index", 1);
        }



        viewModel.getLearnableItems().observe(getViewLifecycleOwner(), items -> {
            if (items != null) {
                learnableItems.clear();
                learnableItems.addAll(items);
                Log.d(TAG, "Loaded " + items.size() + " items");
            } else {
                learnableItems.clear();
                Log.d(TAG, "Loading new flashcards...");
            }

            currentIndex = 0;
            showingFront = true;
            updateUI();
        });

        viewModel.fetchFlashcards(topicId);

        // --- LISTENER ---
        binding.cardContainer.setOnClickListener(v -> flipCard());
        binding.btnPrev.setOnClickListener(v -> {
            if (!isOnCongrats()) {
                currentIndex = Math.max(0, currentIndex - 1);
                showingFront = true;
                updateUI();
            }
        });
        binding.btnNext.setOnClickListener(v -> {
            if (!isOnCongrats()) {
                currentIndex++;
                showingFront = true;
                updateUI();
            } else {
                restoreNavigationBar();
                getParentFragmentManager().popBackStack();
            }
        });
        binding.btnReturnAfterCongrats.setOnClickListener(v -> {
            restoreNavigationBar();
            getParentFragmentManager().popBackStack();
        });
        binding.btnBackTopic.setOnClickListener(v -> {
            restoreNavigationBar();
            getParentFragmentManager().popBackStack();
        });

        updateUI();
        hideNavigationBar();
        return root;
    }

    private boolean isOnCongrats() {
        return learnableItems == null || currentIndex >= learnableItems.size();
    }

    /**
     * ✅ Nâng cấp: Hiển thị phát âm “US” ưu tiên, xử lý null-safety đầy đủ
     */
    private void updateUI() {
        String progress;
        if (learnableItems == null || learnableItems.isEmpty()) {
            progress = "0/0";
        } else if (isOnCongrats()) {
            progress = learnableItems.size() + "/" + learnableItems.size();
        } else {
            progress = (currentIndex + 1) + "/" + learnableItems.size();
        }
        binding.txtProgress.setText("Progress: " + progress);

        if (isOnCongrats()) {
            binding.cardContainer.setVisibility(View.INVISIBLE);
            binding.congratsContainer.setVisibility(View.VISIBLE);
            return;
        }

        binding.cardContainer.setVisibility(View.VISIBLE);
        binding.congratsContainer.setVisibility(View.GONE);

        LearnableItem item = learnableItems.get(currentIndex);
        FlashcardItem word = item.word;
        Definition def = item.definition;

        if (def == null) {
            binding.textTerm.setText(word.getWordText());
            binding.textDefinition.setText("No definition available for this word.");
            binding.textPhonetic.setVisibility(View.GONE);
            binding.textPOS.setVisibility(View.GONE);
            binding.textExample.setVisibility(View.GONE);
            binding.btnAudio.setVisibility(View.GONE);
            binding.imgRegion.setVisibility(View.GONE);
            binding.textVietnameseTerm.setText("N/A");
            binding.textVietnamesePOS.setVisibility(View.GONE);
            return;
        }

        // --- MẶT TRƯỚC ---
        binding.textTerm.setText(word.getWordText());

        // 🔹 Phát âm — chọn “US” trước, fallback sang đầu tiên
        if (word.getPronunciations() != null && !word.getPronunciations().isEmpty()) {
            Pronunciation selectedPron = null;
            for (Pronunciation p : word.getPronunciations()) {
                if ("US".equals(p.getRegion())) {
                    selectedPron = p;
                    break;
                }
            }
            if (selectedPron == null) selectedPron = word.getPronunciations().get(0);

            binding.textPhonetic.setText("'" + selectedPron.getPhoneticSpelling() + "'");
            binding.textPhonetic.setVisibility(View.VISIBLE);

            if ("US".equals(selectedPron.getRegion())) {
                binding.imgRegion.setImageResource(R.drawable.ic_flag_us);
                binding.imgRegion.setVisibility(View.VISIBLE);
            } else {
                binding.imgRegion.setVisibility(View.GONE);
            }

            final String audioUrl = selectedPron.getAudioFileUrl();
            if (audioUrl != null && !audioUrl.isEmpty()) {
                binding.btnAudio.setVisibility(View.VISIBLE);
                binding.btnAudio.setOnClickListener(v -> playAudio(audioUrl));
            } else {
                binding.btnAudio.setVisibility(View.GONE);
            }
        } else {
            binding.textPhonetic.setVisibility(View.GONE);
            binding.btnAudio.setVisibility(View.GONE);
            binding.imgRegion.setVisibility(View.GONE);
        }

        // 🔹 Loại từ
        if (def.getPos() != null) {
            binding.textPOS.setText("(" + def.getPos().getPosName() + ")");
            binding.textPOS.setVisibility(View.VISIBLE);
        } else {
            binding.textPOS.setVisibility(View.GONE);
        }

        // 🔹 Định nghĩa & ví dụ
        binding.textDefinition.setText("Definition: " + def.getDefinitionText());
        if (def.getExamples() != null && !def.getExamples().isEmpty()) {
            binding.textExample.setText("Example: " + def.getExamples().get(0).getExampleSentence());
            binding.textExample.setVisibility(View.VISIBLE);
        } else {
            binding.textExample.setVisibility(View.GONE);
        }

        // --- MẶT SAU ---
        binding.textVietnameseTerm.setText(def.getTranslationText());
        if (def.getPos() != null) {
            binding.textVietnamesePOS.setText("(" + def.getPos().getPosNameVie() + ")");
            binding.textVietnamesePOS.setVisibility(View.VISIBLE);
        } else {
            binding.textVietnamesePOS.setVisibility(View.GONE);
        }

        // 🔹 Reset flip state nếu sang thẻ mới
        if (currentIndex != lastIndex) {
            lastIndex = currentIndex;
            showingFront = true;
            binding.cardContainer.setRotationY(0f);
            binding.frontView.setVisibility(View.VISIBLE);
            binding.backView.setVisibility(View.GONE);
        }
    }

    private void playAudio(String url) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: " + what);
                Toast.makeText(getContext(), "Cannot play audio", Toast.LENGTH_SHORT).show();
                return true;
            });
        } catch (IOException e) {
            Log.e(TAG, "MediaPlayer setDataSource error", e);
            Toast.makeText(getContext(), "Invalid audio source", Toast.LENGTH_SHORT).show();
        }
    }

    private void flipCard() {
        if (isOnCongrats() || isFlipping) return;
        isFlipping = true;

        final View visible = showingFront ? binding.frontView : binding.backView;
        final View hidden = showingFront ? binding.backView : binding.frontView;

        float scale = getResources().getDisplayMetrics().density;
        binding.cardContainer.setCameraDistance(8000 * scale);

        float startAngle = showingFront ? 0 : 180;
        float endAngle = showingFront ? 180 : 0;

        ObjectAnimator flip = ObjectAnimator.ofFloat(binding.cardContainer, "rotationY", startAngle, endAngle);
        flip.setDuration(400);
        flip.setInterpolator(new DecelerateInterpolator());

        flip.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            if (animatedValue >= 90 && visible.getVisibility() == View.VISIBLE) {
                visible.setVisibility(View.GONE);
                hidden.setVisibility(View.VISIBLE);
            }
        });

        flip.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                showingFront = !showingFront;
                isFlipping = false;
            }
        });

        flip.start();
    }

    private void hideNavigationBar() {
        if (getActivity() == null) return;
        View bottomAppBar = getActivity().findViewById(R.id.bottomAppBar);
        View fab = getActivity().findViewById(R.id.fab);
        if (bottomAppBar != null) bottomAppBar.setVisibility(View.GONE);
        if (fab != null) fab.setVisibility(View.GONE);
    }

    private void restoreNavigationBar() {
        if (getActivity() == null) return;
        View bottomAppBar = getActivity().findViewById(R.id.bottomAppBar);
        View fab = getActivity().findViewById(R.id.fab);
        if (bottomAppBar != null) bottomAppBar.setVisibility(View.VISIBLE);
        if (fab != null) fab.setVisibility(View.VISIBLE);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        restoreNavigationBar();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        binding = null;
    }
}
