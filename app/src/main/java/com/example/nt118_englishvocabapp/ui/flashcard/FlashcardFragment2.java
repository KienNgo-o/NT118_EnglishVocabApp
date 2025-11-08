package com.example.nt118_englishvocabapp.ui.flashcard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentFlashcard2Binding;
import com.example.nt118_englishvocabapp.models.Definition;
import com.example.nt118_englishvocabapp.models.Example;
import com.example.nt118_englishvocabapp.models.FlashcardItem;
import com.example.nt118_englishvocabapp.models.POS;

import java.util.ArrayList;
import java.util.List;

public class FlashcardFragment2 extends Fragment {

    private FragmentFlashcard2Binding binding;
    private int topicId = 1; // ID chủ đề thật từ API
    private List<FlashcardItem> flashcards = new ArrayList<>();
    private int currentIndex = 0;
    private boolean showingFront = true;
    private boolean isFlipping = false;
    private int lastIndex = -1;

    private FlashcardViewModel viewModel;

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

        binding.txtTopicTitle.setText(getTopicTitle(topicId));

        // Quan sát dữ liệu flashcards từ ViewModel
        viewModel.getFlashcards().observe(getViewLifecycleOwner(), items -> {
            // 'items' BÂY GIỜ CÓ THỂ LÀ NULL
            if (items != null) {
                // Có dữ liệu mới, ẩn loading
                Log.d("FlashcardFragment2", "Loaded " + items.size() + " flashcards");
                this.flashcards.clear();
                this.flashcards.addAll(items);
            } else {
                // items là null, nghĩa là đang tải
                // Hiển thị loading, xóa thẻ cũ
                this.flashcards.clear();
                Log.d("FlashcardFragment2", "Loading new flashcards...");
            }
            // Dù thế nào cũng cập nhật UI
            this.currentIndex = 0;
            this.showingFront = true;
            updateUI();
        });

        // Gọi API load dữ liệu
        viewModel.fetchFlashcards(topicId);

        // Nút quay lại chủ đề
        binding.btnBackTopic.setOnClickListener(v -> {
            restoreNavigationBar();
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Lật thẻ
        binding.cardContainer.setOnClickListener(v -> flipCard());

        // Nút trước
        binding.btnPrev.setOnClickListener(v -> {
            if (isOnCongrats()) {
                currentIndex = Math.max(0, flashcards.size() - 1);
            } else {
                currentIndex = Math.max(0, currentIndex - 1);
            }
            showingFront = true;
            updateUI();
        });

        // Nút kế tiếp
        binding.btnNext.setOnClickListener(v -> {
            if (!isOnCongrats()) {
                currentIndex++;
                showingFront = true;
                updateUI();
            } else {
                restoreNavigationBar();
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            }
        });

        binding.btnReturnAfterCongrats.setOnClickListener(v -> {
            restoreNavigationBar();
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        updateUI();
        hideNavigationBar();
        return root;
    }

    private boolean isOnCongrats() {
        return flashcards == null || currentIndex >= flashcards.size();
    }

    private void updateUI() {
        String progress;
        if (flashcards == null || flashcards.size() == 0) {
            progress = "0/0";
        } else if (isOnCongrats()) {
            progress = flashcards.size() + "/" + flashcards.size();
        } else {
            progress = (currentIndex + 1) + "/" + flashcards.size();
        }
        binding.txtProgress.setText("Progress: " + progress);

        if (isOnCongrats()) {
            binding.cardContainer.setVisibility(View.INVISIBLE);
            binding.congratsContainer.setVisibility(View.VISIBLE);
        } else {
            binding.cardContainer.setVisibility(View.VISIBLE);
            binding.congratsContainer.setVisibility(View.GONE);

            FlashcardItem item = flashcards.get(currentIndex);

            // Chuẩn bị dữ liệu
            String title = item.getWordText();
            String pos = " ";
            String definition = "N/A";
            String example = " ";
            String vietTitle = item.getWordText();
            String vietDefinition = "N/A";
            String vietPOS = " ";

            if (item.getDefinitions() != null && !item.getDefinitions().isEmpty()) {
                Definition def = item.getDefinitions().get(0);
                definition = def.getDefinitionText();
                vietDefinition = def.getTranslationText();

                if (def.getPos() != null) {
                    pos = def.getPos().getPosName();
                    vietPOS = def.getPos().getPosName();
                }
                if (def.getExamples() != null && !def.getExamples().isEmpty()) {
                    Example ex = def.getExamples().get(0);
                    example = "Example: " + ex.getExampleSentence();
                }
            }

            // Cập nhật mặt trước
            binding.textTerm.setText(title + " (" + pos + ")");
            binding.textDefinition.setText(definition);
            binding.textExample.setText(example);

            // Cập nhật mặt sau
            binding.textVietnameseTerm.setText(vietTitle + " (" + vietPOS + ")");
            binding.textVietnameseDefinition.setText("Định nghĩa: " + vietDefinition);

            if (currentIndex != lastIndex) {
                lastIndex = currentIndex;
                showingFront = true;
                binding.cardContainer.setRotationY(0f);
                binding.frontView.setVisibility(View.VISIBLE);
                binding.backView.setVisibility(View.GONE);
            }
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

        flip.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                showingFront = !showingFront;
                isFlipping = false;
            }
        });

        flip.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            if (value >= 90 && visible.getVisibility() == View.VISIBLE) {
                visible.setVisibility(View.GONE);
                hidden.setVisibility(View.VISIBLE);
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
        binding = null;
    }

    private String getTopicTitle(int idx) {
        switch (idx) {
            case 1: return "Fruits";
            case 2: return "Animals";
            case 3: return "Personalities";
            default: return "Topic";
        }
    }
}
