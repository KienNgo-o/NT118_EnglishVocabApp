package com.example.nt118_englishvocabapp.ui.flashcard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentFlashcard2Binding;

import java.util.ArrayList;
import java.util.List;

public class FlashcardFragment2 extends Fragment {

    private FragmentFlashcard2Binding binding;
    private int topicIndex = 1;
    private List<FlashcardItem> flashcards = new ArrayList<>();
    private int currentIndex = 0;
    private boolean showingFront = true;
    private boolean isFlipping = false;
    private int lastIndex = -1;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFlashcard2Binding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.backView.setRotationY(180f);
        if (getArguments() != null) {
            topicIndex = getArguments().getInt("topic_index", 1);
        }

        // load cards for the topic (only Fruits has cards for now)
        flashcards = getFlashcardsForTopic(topicIndex);

        binding.txtTopicTitle.setText(getTopicTitle(topicIndex));

        binding.btnBackTopic.setOnClickListener(v -> {
            restoreNavigationBar();
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                getParentFragmentManager().popBackStack();
            }
        });

        binding.cardContainer.setOnClickListener(v -> flipCard());

        binding.btnPrev.setOnClickListener(v -> {
            if (isOnCongrats()) {
                currentIndex = Math.max(0, flashcards.size() - 1);
            } else {
                currentIndex = Math.max(0, currentIndex - 1);
            }
            showingFront = true;
            updateUI();
        });

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

        currentIndex = 0;
        showingFront = true;
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
            binding.cardContainer.setVisibility(View.INVISIBLE); // Sử dụng cardContainer thay vì cardWrapper
            binding.congratsContainer.setVisibility(View.VISIBLE);
        } else {
            binding.cardContainer.setVisibility(View.VISIBLE); // Sử dụng cardContainer thay vì cardWrapper
            binding.congratsContainer.setVisibility(View.GONE);

            FlashcardItem item = flashcards.get(currentIndex);
            // Cập nhật mặt trước (Tiếng Anh)
            binding.textTerm.setText(item.title + (item.pos != null ? " (" + item.pos + ")" : ""));
            binding.textDefinition.setText(item.definition);
            binding.textExample.setText(item.example);

            // Cập nhật mặt sau (Tiếng Việt)
            binding.textVietnameseTerm.setText(item.vietnameseTitle + (item.pos != null ? " (danh từ)" : "")); // Giả sử là danh từ
            binding.textVietnameseDefinition.setText("Định nghĩa: " + item.vietnameseDefinition);


            if (currentIndex != lastIndex) {
                lastIndex = currentIndex;
                showingFront = true;
                binding.cardContainer.setRotationY(0f); // Đảm bảo thẻ quay về mặt trước
                binding.frontView.setVisibility(View.VISIBLE);
                binding.backView.setVisibility(View.GONE);
            }
        }
    }

    private void flipCard() {
        if (isOnCongrats() || isFlipping) return;isFlipping = true;

        final View visible = showingFront ? binding.frontView : binding.backView;
        final View hidden = showingFront ? binding.backView : binding.frontView;

        float scale = getResources().getDisplayMetrics().density;
        binding.cardContainer.setCameraDistance(8000 * scale);

        // Xác định góc bắt đầu và kết thúc
        float startAngle = showingFront ? 0 : 180;
        float endAngle = showingFront ? 180 : 0;

        ObjectAnimator flip = ObjectAnimator.ofFloat(binding.cardContainer, "rotationY", startAngle, endAngle);
        flip.setDuration(400);
        flip.setInterpolator(new DecelerateInterpolator());

        flip.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Hiển thị view ẩn ở giữa animation
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                showingFront = !showingFront;
                isFlipping = false;
            }
        });

        flip.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            // Lật view tại điểm giữa của animation (90 độ)
            if (animatedValue >= 90 && visible.getVisibility() == View.VISIBLE) {
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

    // --- Data model and provider ---
    private static class FlashcardItem {
        String title;
        String pos;
        String definition;
        String example;
        String vietnameseTitle; // Thêm trường này
        String vietnameseDefinition; // Thêm trường này

        FlashcardItem(String title, String pos, String definition, String example, String vietnameseTitle, String vietnameseDefinition) {
            this.title = title;
            this.pos = pos;
            this.definition = definition;
            this.example = example;
            this.vietnameseTitle = vietnameseTitle; // Khởi tạo
            this.vietnameseDefinition = vietnameseDefinition; // Khởi tạo
        }
    }

    private List<FlashcardItem> getFlashcardsForTopic(int topicIdx) {
        List<FlashcardItem> list = new ArrayList<>();
        if (topicIdx == 1) { // Chỉ mục Fruits
            list.add(new FlashcardItem(
                    "Watermelon",
                    "noun",
                    "A large, round or oval-shaped fruit with dark green skin, sweet pink flesh, and a lot of black seeds.",
                    "Example: Watermelon is my favorite fruit in the summer.",
                    "Dưa hấu", // Tiêu đề tiếng Việt
                    "Một loại quả to, hình tròn hoặc oval, có vỏ màu xanh đậm, ruột hồng ngọt và có nhiều hạt đen." // Định nghĩa tiếng Việt
            ));
            list.add(new FlashcardItem(
                    "Tomato",
                    "noun",
                    "A round, red fruit with a lot of seeds, eaten cooked or uncooked as a vegetable, for example in salads or sauces.",
                    "Example: Cut the tomato in half and scoop out the seeds.",
                    "Cà chua", // Tiêu đề tiếng Việt
                    "Một loại quả tròn, màu đỏ, có nhiều hạt, được ăn chín hoặc sống như một loại rau, ví dụ trong salad hoặc nước sốt." // Định nghĩa tiếng Việt
            ));
        }
        // các chủ đề khác trả về danh sách rỗng -> hiển thị congrats ngay lập tức
        return list;
    }


    private String getTopicTitle(int idx) {
        switch (idx) {
            case 1: return "Fruits";
            case 2: return "Animals";
            case 3: return "Personalities";
            case 4: return "Careers";
            case 5: return "Appearances";
            case 6: return "Travel";
            default: return "Topic";
        }
    }
}
