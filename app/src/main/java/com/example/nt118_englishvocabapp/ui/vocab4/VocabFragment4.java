package com.example.nt118_englishvocabapp.ui.vocab4;

import android.os.Bundle;
import android.util.Log;
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
import com.example.nt118_englishvocabapp.databinding.FragmentVocab4Binding; // 👈 THÊM

import com.example.nt118_englishvocabapp.models.WordDetail; // 👈 THÊM
import com.example.nt118_englishvocabapp.models.WordForms;
import com.example.nt118_englishvocabapp.models.RelatedWord;
import com.example.nt118_englishvocabapp.ui.vocab2.VocabFragment2;
import com.example.nt118_englishvocabapp.ui.vocab3.VocabFragment3;
import com.example.nt118_englishvocabapp.ui.vocab3.VocabWordViewModel; // 👈 THÊM
import com.example.nt118_englishvocabapp.ui.vocab5.VocabFragment5;

import java.util.List;
import java.util.stream.Collectors;

public class VocabFragment4 extends Fragment {

    private static final String TAG = "VocabFragment4";
    private FragmentVocab4Binding binding; // 👈 Dùng ViewBinding
    private VocabWordViewModel viewModel; // 👈 Dùng ViewModel đã chia sẻ
    // Nếu được truyền topic index từ VocabFragment2/VocabFragment3, lưu để trả lại khi pop
    private int topicIndex = -1;

    public VocabFragment4() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Dùng ViewBinding
        binding = FragmentVocab4Binding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Lấy topic_index nếu có (được truyền từ VocabFragment3)
        Bundle args = getArguments();
        if (args != null && args.containsKey("topic_index")) {
            topicIndex = args.getInt("topic_index", -1);
        }

        // 1. Lấy ViewModel được chia sẻ (từ Activity)
        viewModel = new ViewModelProvider(requireActivity()).get(VocabWordViewModel.class);

        // 2. Cài đặt các Tab
        setupTabs(root, container != null ? container.getId() : android.R.id.content);
        binding.tabForms.setSelected(true); // Đặt tab này là active

        // 3. Cài đặt nút Return
        setupReturnButton(root, container != null ? container.getId() : android.R.id.content);

        // 4. Theo dõi (Observe) LiveData
        observeViewModel();

        Toast.makeText(getContext(), "Vocab Fragment 4 Opened!", Toast.LENGTH_SHORT).show();
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
                binding.txtNounForms.setText("None.");
                binding.txtVerbForms.setText("None.");
                binding.txtAdjectiveForms.setText("None.");
                binding.txtAdverbForms.setText("None.");
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

        // 2. Lấy dữ liệu WordForms
        WordForms forms = detail.getWordForms();
        if (forms != null) {
            // Điền Noun
            binding.txtNounForms.setText(formatRelatedWords(forms.noun));
            // Điền Verb
            binding.txtVerbForms.setText(formatRelatedWords(forms.verb));
            // Điền Adjective
            binding.txtAdjectiveForms.setText(formatRelatedWords(forms.adjective));
            // Điền Adverb
            binding.txtAdverbForms.setText(formatRelatedWords(forms.adverb));
        } else {
            // Xử lý nếu API trả về WordForms rỗng
            binding.txtNounForms.setText("None.");
            binding.txtVerbForms.setText("None.");
            binding.txtAdjectiveForms.setText("None.");
            binding.txtAdverbForms.setText("None.");
        }
    }

    /**
     * Hàm helper để biến List<RelatedWord> thành 1 String
     * Ví dụ: [happy, happiness] -> "happy, happiness"
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
            // Không truyền Bundle nữa, vì các Fragment khác cũng sẽ đọc từ ViewModel

            if (v.getId() == R.id.tab_definition) {
                // Nếu ViewModel đang giữ 1 wordId, truyền nó trong Bundle để tránh lỗi "No Word ID found".
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
                // Đang ở tab này, không làm gì
            } else if (v.getId() == R.id.tab_synonyms) {
                int currentId = viewModel.getCurrentWordId();
                VocabFragment5 f5 = new VocabFragment5();
                if (currentId > 0) {
                    Bundle b = new Bundle();
                    b.putInt(VocabFragment3.ARG_WORD_ID, currentId);
                    if (topicIndex > 0) b.putInt("topic_index", topicIndex);
                    f5.setArguments(b);
                }
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(hostId, f5)
                        .addToBackStack(null)
                        .commit();
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
                // Always navigate directly to VocabFragment2 for a predictable behavior
                androidx.fragment.app.FragmentManager fm = requireActivity().getSupportFragmentManager();
                try {
                    // Clear entire back stack so the new VocabFragment2 is the only fragment
                    try { fm.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE); } catch (Exception ignored) {}
                    try { fm.executePendingTransactions(); } catch (Exception ignored) {}

                    VocabFragment2 vf2 = new VocabFragment2();
                    if (topicIndex > 0) {
                        Bundle b = new Bundle();
                        b.putInt("topic_index", topicIndex);
                        vf2.setArguments(b);
                    }
                    fm.beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.frame_layout, vf2)
                            .commitAllowingStateLoss();
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