package com.example.nt118_englishvocabapp.ui.quiz;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.adapters.QuizOptionAdapter;
import com.example.nt118_englishvocabapp.databinding.FragmentQuizGameBinding;
import com.example.nt118_englishvocabapp.models.QuizData;
import com.example.nt118_englishvocabapp.models.QuizSubmission;
import com.example.nt118_englishvocabapp.util.StreakManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * QuizGameFragment - merged & completed version
 * - Bao gồm đầy đủ logic matching (select/unmatch, badges, highlight)
 * - Lưu đáp án trước khi Next/Submit
 * - Play audio, quit dialog, observe ViewModel...
 */
public class QuizGameFragment extends Fragment {

    private static final String TAG = "QuizGameFragment";
    private FragmentQuizGameBinding binding;
    private QuizViewModel viewModel;
    private int topicId;
    private MediaPlayer mediaPlayer;
    private QuizOptionAdapter optionAdapter;
    private List<android.widget.EditText> blankInputs = new ArrayList<>();
    // --- BIẾN CHO LOGIC NỐI TỪ ---
    // Lưu cặp đã nối: Key = ImageUrl, Value = WordText
    private Map<String, String> currentMatches = new HashMap<>();
    // Lưu số thứ tự badge: Key = ImageUrl hoặc WordText, Value = Số (1, 2, 3...)
    private Map<String, Integer> pairedBadges = new HashMap<>();

    private String selectedImageUrl = null; // Hình đang được chọn tạm thời
    private View selectedLeftView = null;   // View hình đang chọn
    private int nextBadgeNumber = 1;        // Số thứ tự tiếp theo để gán

    // Để dễ dàng tìm view khi cần reset (unmatch)
    private List<View> leftViews = new ArrayList<>();
    private List<View> rightViews = new ArrayList<>();
    private StreakManager streakManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentQuizGameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        streakManager = new StreakManager(requireContext());

        viewModel = new ViewModelProvider(requireActivity()).get(QuizViewModel.class);

        if (getArguments() != null) {
            topicId = getArguments().getInt("topic_id");
            // Reset lại bài thi mỗi khi vào màn hình này
            viewModel.fetchQuiz(topicId);
        }

        setupRecyclerView();
        setupListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        optionAdapter = new QuizOptionAdapter();
        binding.recyclerOptions.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.recyclerOptions.setAdapter(optionAdapter);
    }

    private void setupListeners() {
        binding.btnQuit.setOnClickListener(v -> showQuitDialog());

        binding.btnNextQuestion.setOnClickListener(v -> {
            saveCurrentAnswer();
            Integer currentIdx = viewModel.getCurrentQuestionIndex().getValue();
            QuizData data = viewModel.getQuizData().getValue();

            if (currentIdx == null) return;

            if (data != null && currentIdx < data.questions.size() - 1) {
                viewModel.nextQuestion();
            } else {
                submitQuiz();
            }
        });
    }

    private void showQuitDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_quit_quiz);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View btnCancel = dialog.findViewById(R.id.btnQuitCancel);
        View btnConfirm = dialog.findViewById(R.id.btnQuitConfirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            getParentFragmentManager().popBackStack(); // Thoát Fragment
        });

        dialog.show();
    }

    private void observeViewModel() {
        viewModel.getQuizData().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                binding.progressBar.setMax(data.questions.size());
                // Render câu đầu tiên nếu là lần đầu
                Integer idx = viewModel.getCurrentQuestionIndex().getValue();
                if (idx != null && idx == 0) {
                    renderQuestion(data.questions.get(0), 1, data.questions.size());
                }
            }
        });

        viewModel.getCurrentQuestionIndex().observe(getViewLifecycleOwner(), index -> {
            QuizData data = viewModel.getQuizData().getValue();
            if (data != null && index < data.questions.size()) {
                renderQuestion(data.questions.get(index), index + 1, data.questions.size());
            }
        });

        viewModel.getTimeRemaining().observe(getViewLifecycleOwner(), seconds -> {
            if (seconds == null) return;
            int min = seconds / 60;
            int sec = seconds % 60;
            binding.txtTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
            if (seconds < 30) binding.txtTimer.setTextColor(Color.RED);
            if (seconds == 0) {
                Toast.makeText(getContext(), "Time's up! Submitting...", Toast.LENGTH_SHORT).show();
                submitQuiz();
            }
        });

        viewModel.getQuizResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                // Mark today active when user finishes a quiz (counts toward streak)
                try {
                    if (streakManager != null) {
                        streakManager.markTodayActive();
                    }
                } catch (Exception ignored) {}

                Toast.makeText(getContext(), "Score: " + result.score + "/100. Passed: " + result.passed, Toast.LENGTH_LONG).show();
                getParentFragmentManager().popBackStack();
            }
        });
        // 4. Kết quả nộp bài -> HIỂN THỊ DIALOG
        viewModel.getQuizResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result.passed) {
                    showPassedDialog(result.score);
                } else {
                    showFailedDialog(result.score);
                }
            }
        });
    }
    private void showPassedDialog(int score) {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_quiz_passed);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.setCancelable(false); // Không cho bấm ra ngoài để thoát

        TextView tvScore = dialog.findViewById(R.id.tv_score_passed);
        tvScore.setText(String.format(Locale.getDefault(), "Your score: %d/100", score));

        View btnConfirm = dialog.findViewById(R.id.btn_confirm_passed);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();

                // 🛑 ĐIỀU HƯỚNG AN TOÀN:
                // Sử dụng onBackPressed() thay vì popBackStack() trực tiếp
                // Nó mô phỏng hành động nhấn nút Back vật lý, an toàn hơn
                if (getActivity() != null) {
                    getActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            });
        } else {
            // Nếu không tìm thấy nút (hiếm gặp), log lỗi để biết
            Log.e(TAG, "Không tìm thấy nút btn_confirm_failed trong dialog");
        }

        dialog.show();
    }

    private void showFailedDialog(int score) {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_quiz_failed);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.setCancelable(false);

        TextView tvScore = dialog.findViewById(R.id.tv_score_failed);
        if (tvScore != null) {
            tvScore.setText(String.format(Locale.getDefault(), "Your score: %d/100", score));
        }

        // Tìm nút bấm
        View btnConfirm = dialog.findViewById(R.id.btn_confirm_failed);

        // 🛑 KIỂM TRA NULL ĐỂ TRÁNH CRASH
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();

                // 🛑 ĐIỀU HƯỚNG AN TOÀN:
                // Sử dụng onBackPressed() thay vì popBackStack() trực tiếp
                // Nó mô phỏng hành động nhấn nút Back vật lý, an toàn hơn
                if (getActivity() != null) {
                    getActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            });
        } else {
            // Nếu không tìm thấy nút (hiếm gặp), log lỗi để biết
            Log.e(TAG, "Không tìm thấy nút btn_confirm_failed trong dialog");
        }

        dialog.show();
    }
    // --- CORE LOGIC: RENDER GIAO DIỆN ---
    private void renderQuestion(QuizData.Question q, int currentNum, int totalNum) {
        // --- 1. RESET TOÀN BỘ GIAO DIỆN (QUAN TRỌNG) ---

        // Ẩn tất cả các container trả lời
        binding.recyclerOptions.setVisibility(View.GONE);

        // 🛑 SỬA: Thêm dòng này để ẩn container điền từ
        binding.layoutFillBlankContainer.setVisibility(View.GONE);
        binding.layoutFillBlankContainer.removeAllViews(); // Xóa sạch các ô cũ
        blankInputs.clear(); // Xóa danh sách tham chiếu

        // Reset Matching
        binding.layoutMatching.setVisibility(View.GONE);
        binding.layoutMatching.removeAllViews(); // Xóa các thẻ nối cũ

        // Reset Media
        binding.imgQuestion.setVisibility(View.GONE);
        binding.btnPlayAudio.setVisibility(View.GONE);

        // Reset các biến trạng thái
        optionAdapter.clearSelection();

        currentMatches.clear();
        pairedBadges.clear();
        leftViews.clear();
        rightViews.clear();
        selectedImageUrl = null;
        selectedLeftView = null;
        nextBadgeNumber = 1;

        // --- 2. CẬP NHẬT TIÊU ĐỀ & THANH TIẾN ĐỘ ---
        binding.progressBar.setProgress(currentNum, true);

        if ("FILL_BLANK".equals(q.questionType)) {
            binding.txtQuestionPrompt.setText("Fill in the blank:");
        } else {
            binding.txtQuestionPrompt.setText((q.prompt != null) ? q.prompt : "Answer the question:");
        }

        // Update Nút Next/Submit
        if (currentNum == totalNum) {
            binding.btnNextQuestion.setText("Submit");
            binding.btnNextQuestion.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.correct_green));
        } else {
            binding.btnNextQuestion.setText("Next");
            binding.btnNextQuestion.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.purple_700));
        }

        // --- 3. HIỂN THỊ MEDIA (NẾU CÓ) ---
        if (q.imageUrl != null && !q.imageUrl.isEmpty()) {
            binding.imgQuestion.setVisibility(View.VISIBLE);
            Glide.with(this).load(q.imageUrl).into(binding.imgQuestion);
        }
        if (q.audioUrl != null && !q.audioUrl.isEmpty()) {
            binding.btnPlayAudio.setVisibility(View.VISIBLE);
            binding.btnPlayAudio.setOnClickListener(v -> playAudio(q.audioUrl));
        }

        // --- 4. HIỂN THỊ VÙNG TRẢ LỜI TƯƠNG ỨNG ---
        switch (q.questionType) {
            case "LISTEN_CHOOSE_IMG":
            case "IMG_CHOOSE_TEXT":
                binding.recyclerOptions.setVisibility(View.VISIBLE);
                optionAdapter.setOptions(q.options, optionId -> {});
                break;

            case "FILL_BLANK":
                // Gọi hàm tạo ô nhập liệu
                setupFillBlank(q);
                break;

            case "MATCH_PAIRS":
                // Gọi hàm tạo thẻ nối
                setupMatchingUI(q.pairs);
                break;
        }
    }

    // --- LOGIC: XỬ LÝ DẠNG MATCHING (NỐI TỪ) ---
    private void setupMatchingUI(List<QuizData.Pair> pairs) {
        if (pairs == null || pairs.isEmpty()) return;

        binding.layoutMatching.setVisibility(View.VISIBLE);
        binding.layoutMatching.removeAllViews();

        // Xáo trộn
        List<QuizData.Pair> leftSide = new ArrayList<>(pairs);
        List<QuizData.Pair> rightSide = new ArrayList<>(pairs);
        Collections.shuffle(leftSide);
        Collections.shuffle(rightSide);

        // Layout container
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setWeightSum(2);

        // Cột Trái & Phải
        LinearLayout colLeft = new LinearLayout(getContext());
        colLeft.setOrientation(LinearLayout.VERTICAL);
        colLeft.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout colRight = new LinearLayout(getContext());
        colRight.setOrientation(LinearLayout.VERTICAL);
        colRight.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Render Cột Trái (Hình)
        for (QuizData.Pair p : leftSide) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_matching_card_left, colLeft, false);
            ImageView img = view.findViewById(R.id.img_match);
            // load into the item image view instead of the fragment's main image
            Glide.with(this).load(p.imageUrl).into(img);

            // Gán tag để tìm lại sau này
            view.setTag(p.imageUrl);
            view.setOnClickListener(v -> onLeftItemClicked(p.imageUrl, view));

            leftViews.add(view);
            colLeft.addView(view);
        }

        // Render Cột Phải (Chữ)
        for (QuizData.Pair p : rightSide) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_matching_card_right, colRight, false);
            TextView txt = view.findViewById(R.id.txt_match);
            txt.setText(p.wordText);

            view.setTag(p.wordText);
            view.setOnClickListener(v -> onRightItemClicked(p.wordText, view));

            rightViews.add(view);
            colRight.addView(view);
        }

        container.addView(colLeft);
        container.addView(colRight);
        binding.layoutMatching.addView(container);
    }

    private void onLeftItemClicked(String imageUrl, View view) {
        // [NEW] Nếu hình này ĐÃ ĐƯỢC NỐI -> Gỡ bỏ (Unmatch)
        if (currentMatches.containsKey(imageUrl)) {
            String matchedWord = currentMatches.get(imageUrl);
            unmatchPair(imageUrl, matchedWord);
            return;
        }

        // Nếu chưa nối -> Chọn (Select)
        // Reset view cũ nếu đang chọn dở
        if (selectedLeftView != null) {
            selectedLeftView.setBackgroundResource(R.drawable.bg_matching_card_normal);
        }

        selectedImageUrl = imageUrl;
        selectedLeftView = view;

        // Highlight màu tím (đang chọn)
        view.setBackgroundResource(R.drawable.bg_matching_card_selected);
    }

    // 2. CLICK CHỮ (PHẢI)
    private void onRightItemClicked(String wordText, View view) {
        // [NEW] Nếu chữ này ĐÃ ĐƯỢC NỐI -> Gỡ bỏ (Unmatch)
        if (currentMatches.containsValue(wordText)) {
            // Tìm hình tương ứng để gỡ
            String linkedImage = null;
            for (Map.Entry<String, String> entry : currentMatches.entrySet()) {
                if (entry.getValue().equals(wordText)) {
                    linkedImage = entry.getKey();
                    break;
                }
            }
            if (linkedImage != null) unmatchPair(linkedImage, wordText);
            return;
        }

        // Nếu chưa nối -> Kiểm tra xem có hình nào đang được chọn không
        if (selectedImageUrl == null) {
            Toast.makeText(getContext(), "Please select an image first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- THỰC HIỆN NỐI (MATCH) ---
        currentMatches.put(selectedImageUrl, wordText);
        pairedBadges.put(selectedImageUrl, nextBadgeNumber);
        pairedBadges.put(wordText, nextBadgeNumber);

        // Cập nhật UI: Hiện Badge số và đổi màu Xanh
        updateItemUI(selectedLeftView, nextBadgeNumber, true);
        updateItemUI(view, nextBadgeNumber, true);

        nextBadgeNumber++;

        // Reset selection
        selectedImageUrl = null;
        selectedLeftView = null;
    }

    // [NEW] HÀM GỬ BỎ CẶP ĐÃ NỐI
    private void unmatchPair(String imageUrl, String wordText) {
        // Xóa dữ liệu
        currentMatches.remove(imageUrl);
        pairedBadges.remove(imageUrl);
        pairedBadges.remove(wordText);

        // Tìm lại View của Hình và Chữ để reset UI
        for (View v : leftViews) {
            if (v.getTag() != null && v.getTag().equals(imageUrl)) {
                updateItemUI(v, 0, false); // Reset về trắng, ẩn badge
            }
        }
        for (View v : rightViews) {
            if (v.getTag() != null && v.getTag().equals(wordText)) {
                updateItemUI(v, 0, false); // Reset về trắng, ẩn badge
            }
        }
    }

    // Hàm helper để đổi dp sang px
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
    private void setupFillBlank(QuizData.Question q) {
        // Sử dụng FlexboxLayout
        com.google.android.flexbox.FlexboxLayout container = binding.getRoot().findViewById(R.id.layout_fill_blank_container);
        container.setVisibility(View.VISIBLE);
        container.removeAllViews();
        blankInputs.clear();

        // Lấy nội dung câu hỏi thực sự (ví dụ: "y__low")
        // Lưu ý: q.prompt lúc này chứa "y__low" (từ DB), còn txtQuestionPrompt ở trên đã set là "Fill in the blank:"
        String content = q.prompt;
        if (content == null) content = "";

        char[] chars = content.toCharArray();

        for (char c : chars) {
            if (c == '_') {
                // --- TẠO Ô NHẬP ---
                android.widget.EditText edt = new android.widget.EditText(getContext());

                // Kích thước ô nhập
                com.google.android.flexbox.FlexboxLayout.LayoutParams params = new com.google.android.flexbox.FlexboxLayout.LayoutParams(
                        dpToPx(30), // Rộng 40dp
                        dpToPx(45)  // Cao 50dp
                );
                // Margin rộng hơn một chút để thoáng
                params.setMargins(dpToPx(2), dpToPx(8), dpToPx(2), dpToPx(8));
                edt.setLayoutParams(params);

                // Style
                edt.setGravity(android.view.Gravity.CENTER);
                edt.setTextSize(24);
                edt.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_700));
                edt.setMaxLines(1);
                edt.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(1) });
                edt.setBackgroundResource(R.drawable.bg_edit_text_underline);
                edt.setPadding(0, 0, 0, 0);

                // Tự động in hoa
                edt.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

                blankInputs.add(edt);
                container.addView(edt);

            } else if (c == ' ') {
                // Khoảng trắng
                View space = new View(getContext());
                com.google.android.flexbox.FlexboxLayout.LayoutParams params = new com.google.android.flexbox.FlexboxLayout.LayoutParams(dpToPx(20), 1);
                space.setLayoutParams(params);
                container.addView(space);

            } else {
                // --- TẠO CHỮ CỐ ĐỊNH ---
                TextView tv = new TextView(getContext());
                tv.setText(String.valueOf(c));
                tv.setTextSize(24);
                tv.setTextColor(Color.BLACK);
                tv.setTypeface(null, android.graphics.Typeface.BOLD);

                com.google.android.flexbox.FlexboxLayout.LayoutParams params = new com.google.android.flexbox.FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                // Căn chỉnh margin để chữ nằm thẳng hàng với ô nhập
                params.setMargins(0, dpToPx(16), 0, dpToPx(16));

                tv.setLayoutParams(params);
                container.addView(tv);
            }
        }

        // Focus ô đầu tiên
        if (!blankInputs.isEmpty()) {
            blankInputs.get(0).requestFocus();
        }
    }
    // Hàm cập nhật giao diện item (Hiện/Ẩn Badge, Đổi màu)
    private void updateItemUI(View view, int badgeNumber, boolean isMatched) {
        TextView badge = view.findViewById(R.id.txt_badge);
        if (badge == null) {
            // item layout của bạn phải có txt_badge; nếu không có, tạo 1 TextView trong layout xml tương ứng.
            return;
        }
        if (isMatched) {
            view.setBackgroundResource(R.drawable.bg_matching_card_matched); // Xanh lá
            badge.setText(String.valueOf(badgeNumber));
            badge.setVisibility(View.VISIBLE);
        } else {
            view.setBackgroundResource(R.drawable.bg_matching_card_normal); // Trắng
            badge.setVisibility(View.GONE);
        }
    }

    private boolean isImageMatched(String imageUrl) {
        return currentMatches.containsKey(imageUrl);
    }

    // --- LƯU ĐÁP ÁN TRƯỚC KHI NEXT ---
    private void saveCurrentAnswer() {
        Integer currentIdx = viewModel.getCurrentQuestionIndex().getValue();
        QuizData data = viewModel.getQuizData().getValue();
        if (data == null || currentIdx == null) return;

        QuizData.Question q = data.questions.get(currentIdx);
        int qId = q.questionId;

        switch (q.questionType) {
            case "LISTEN_CHOOSE_IMG":
            case "IMG_CHOOSE_TEXT":
                int selectedOptId = optionAdapter.getSelectedOptionId();
                if (selectedOptId != -1) {
                    viewModel.saveAnswer(qId, new QuizSubmission.Answer(qId, selectedOptId));
                }
                break;
            case "FILL_BLANK":
                StringBuilder fullAnswer = new StringBuilder();
                for (EditText edt : blankInputs) {
                    fullAnswer.append(edt.getText().toString().trim());
                }
                // fullAnswer sẽ là "el" (nếu user nhập e và l)
                // Hoặc bạn cần ghép với các ký tự có sẵn để ra từ hoàn chỉnh "yellow"
                // Tùy thuộc vào logic chấm điểm của bạn ở Server.
                // Nếu Server so sánh với "yellow", bạn cần logic ghép lại.
                // Nếu Server so sánh với "el", thì gửi "el".

                if (fullAnswer.length() > 0) {
                    viewModel.saveAnswer(qId, new QuizSubmission.Answer(qId, fullAnswer.toString()));
                }
                break;
            case "MATCH_PAIRS":
                if (!currentMatches.isEmpty()) {
                    List<QuizSubmission.PairSubmission> pairs = new ArrayList<>();
                    for (Map.Entry<String, String> entry : currentMatches.entrySet()) {
                        pairs.add(new QuizSubmission.PairSubmission(entry.getKey(), entry.getValue()));
                    }
                    viewModel.saveAnswer(qId, new QuizSubmission.Answer(qId, pairs));
                }
                break;
        }
    }

    private void submitQuiz() {
        saveCurrentAnswer();
        viewModel.submitQuiz(topicId);
    }

    private void playAudio(String url) {
        if (mediaPlayer != null) mediaPlayer.release();
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
        } catch (IOException e) {
            Log.e(TAG, "Audio error", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mediaPlayer != null) mediaPlayer.release();
        binding = null;
    }
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            // Tìm và ẩn BottomNavigationView
            // (Lưu ý: ID phải khớp với ID trong activity_main.xml của bạn)
            View navBar = getActivity().findViewById(R.id.bottomNavigationView);
            if (navBar != null) navBar.setVisibility(View.GONE);

            // Nếu bạn dùng BottomAppBar + FAB (như ở các màn hình khác) thì ẩn luôn
            View bottomAppBar = getActivity().findViewById(R.id.bottomAppBar);
            View fab = getActivity().findViewById(R.id.fab);
            if (bottomAppBar != null) bottomAppBar.setVisibility(View.GONE);
            if (fab != null) fab.setVisibility(View.GONE);
        }
    }

    // 2. Khi thoát khỏi màn hình này -> Hiện lại thanh điều hướng
    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() != null) {
            View navBar = getActivity().findViewById(R.id.bottomNavigationView);
            if (navBar != null) navBar.setVisibility(View.VISIBLE);

            // Hiện lại BottomAppBar + FAB
            View bottomAppBar = getActivity().findViewById(R.id.bottomAppBar);
            View fab = getActivity().findViewById(R.id.fab);
            if (bottomAppBar != null) bottomAppBar.setVisibility(View.VISIBLE);
            if (fab != null) fab.setVisibility(View.VISIBLE);
        }
    }
}
