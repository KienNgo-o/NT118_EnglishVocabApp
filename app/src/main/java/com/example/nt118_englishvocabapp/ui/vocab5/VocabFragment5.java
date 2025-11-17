package com.example.nt118_englishvocabapp.ui.vocab5;

import android.os.Bundle;
import android.text.TextUtils; // 👈 THÊM
import android.util.Log; // 👈 THÊM
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // 👈 THÊM

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentVocab5Binding; // 👈 THÊM
import com.example.nt118_englishvocabapp.models.RelatedWord; // 👈 THÊM
import com.example.nt118_englishvocabapp.models.WordDetail; // 👈 THÊM
import com.example.nt118_englishvocabapp.ui.vocab2.VocabFragment2;
import com.example.nt118_englishvocabapp.ui.vocab3.VocabFragment3;
import com.example.nt118_englishvocabapp.ui.vocab3.VocabWordViewModel; // 👈 THÊM
import com.example.nt118_englishvocabapp.ui.vocab4.VocabFragment4;

import java.util.List; // 👈 THÊM
import java.util.stream.Collectors; // 👈 THÊM

public class VocabFragment5 extends Fragment {

    private static final String TAG = "VocabFragment5";
    private FragmentVocab5Binding binding; // 👈 Dùng ViewBinding
    private VocabWordViewModel viewModel; // 👈 Dùng ViewModel đã chia sẻ
    private int topicIndex = -1;

    public VocabFragment5() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Dùng ViewBinding
        binding = FragmentVocab5Binding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Lấy topic_index nếu có
        Bundle args = getArguments();
        if (args != null && args.containsKey("topic_index")) {
            topicIndex = args.getInt("topic_index", -1);
        }

        // 1. Lấy ViewModel được chia sẻ (từ Activity)
        viewModel = new ViewModelProvider(requireActivity()).get(VocabWordViewModel.class);

        // 2. Cài đặt các Tab
        setupTabs(root, container != null ? container.getId() : android.R.id.content);
        binding.tabSynonyms.setSelected(true); // Đặt tab này là active

        // 3. Cài đặt nút Return
        setupReturnButton(root, container != null ? container.getId() : android.R.id.content);

        // 4. Theo dõi (Observe) LiveData
        observeViewModel();

        Toast.makeText(getContext(), "Vocab Fragment 5 Opened!", Toast.LENGTH_SHORT).show();
        return root;
    }

    /**
     * Theo dõi LiveData từ ViewModel
     */
    private void observeViewModel() {
        viewModel.getWordDetail().observe(getViewLifecycleOwner(), wordDetail -> {
            if (wordDetail != null) {
                // Cập nhật UI ngay khi có dữ liệu
                updateUi(wordDetail);
            } else {
                // Dữ liệu đang tải (hoặc bị lỗi), hiển thị "None."
                binding.wordText.setText("Loading...");
                binding.wordType.setText("");
                binding.txtSynonyms.setText("None."); // 👈 THÊM ID NÀY VÀO XML
                binding.txtAntonyms.setText("None."); // 👈 THÊM ID NÀY VÀO XML
            }
        });
    }

    /**
     * Điền dữ liệu từ WordDetail vào các View
     */
    private void updateUi(WordDetail detail) {
        Log.d(TAG, "updateUi called with word: " + detail.getWordText());

        // 1. Cập nhật thanh Word/Type (lấy từ Definition đầu tiên)
        binding.wordText.setText(detail.getWordText());
        if (detail.getDefinitions() != null && !detail.getDefinitions().isEmpty()) {
            if (detail.getDefinitions().get(0).getPos() != null) {
                binding.wordType.setText("(" + detail.getDefinitions().get(0).getPos().getPosName() + ")");
            }
        }

        // 2. Lấy dữ liệu Synonyms (Đồng nghĩa)
        String synonymsText = formatRelatedWords(detail.getSynonyms());
        binding.txtSynonyms.setText(synonymsText); // 👈 THÊM ID NÀY VÀO XML

        // 3. Lấy dữ liệu Antonyms (Trái nghĩa)
        String antonymsText = formatRelatedWords(detail.getAntonyms());
        binding.txtAntonyms.setText(antonymsText); // 👈 THÊM ID NÀY VÀO XML
    }

    /**
     * Hàm helper để biến List<RelatedWord> thành 1 String
     * Ví dụ: [word1, word2] -> "word1, word2"
     */
    private String formatRelatedWords(List<RelatedWord> words) {
        if (words == null || words.isEmpty()) {
            return "None.";
        }
        // Dùng Java Stream để nối các từ (Java 8+)
        return words.stream()
                .map(w -> w.wordText)
                .collect(Collectors.joining(", "));
    }

    /**
     * Cài đặt logic cho 3 Tab
     */
    private void setupTabs(View root, int hostId) {
        View.OnClickListener tabClick = v -> {
            // Không cần truyền Bundle

            if (v.getId() == R.id.tab_definition) {
                int currentId = viewModel.getCurrentWordId();
                VocabFragment3 f3 = new VocabFragment3();
                if (currentId > 0) {
                    Bundle b = new Bundle();
                    b.putInt(VocabFragment3.ARG_WORD_ID, currentId);
                    if (topicIndex > 0) b.putInt("topic_index", topicIndex);
                    f3.setArguments(b);
                }
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(hostId, f3)
                        .addToBackStack(null)
                        .commit();
            } else if (v.getId() == R.id.tab_forms) {
                int currentId = viewModel.getCurrentWordId();
                VocabFragment4 f4 = new VocabFragment4();
                if (currentId > 0) {
                    Bundle b = new Bundle();
                    b.putInt(VocabFragment3.ARG_WORD_ID, currentId);
                    if (topicIndex > 0) b.putInt("topic_index", topicIndex);
                    f4.setArguments(b);
                }
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(hostId, f4)
                        .addToBackStack(null)
                        .commit();
            } else if (v.getId() == R.id.tab_synonyms) {
                // Đang ở tab này, không làm gì
            }
        };

        binding.tabDefinition.setOnClickListener(tabClick);
        binding.tabForms.setOnClickListener(tabClick);
        binding.tabSynonyms.setOnClickListener(tabClick);
    }

    /**
     * Cài đặt nút Return (Giữ nguyên logic của bạn)
     */
    private void setupReturnButton(View root, int hostId) {
        ImageButton btnReturn = root.findViewById(R.id.btn_return);
        if (btnReturn != null) {
            btnReturn.setOnClickListener(v -> {
                if (getActivity() == null) return;
                v.setEnabled(false);
                // Always navigate directly to a fresh VocabFragment2 (no back stack pop) to ensure
                // the returned VocabFragment2 has predictable return behavior.
                androidx.fragment.app.FragmentManager fm = requireActivity().getSupportFragmentManager();
                try {
                    VocabFragment2 vf2 = new VocabFragment2();
                    if (topicIndex > 0) {
                        Bundle b = new Bundle();
                        b.putInt("topic_index", topicIndex);
                        vf2.setArguments(b);
                    }
                    fm.beginTransaction().setReorderingAllowed(true).replace(R.id.frame_layout, vf2).commitAllowingStateLoss();
                } catch (Exception ignored) {
                    if (getActivity() != null) requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Quan trọng: Dọn dẹp ViewBinding
    }
}