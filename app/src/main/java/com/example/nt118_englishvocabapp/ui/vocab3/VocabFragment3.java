package com.example.nt118_englishvocabapp.ui.vocab3;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentVocab3Binding; // 👈 Đảm bảo import này đúng
import com.example.nt118_englishvocabapp.models.Definition;
import com.example.nt118_englishvocabapp.models.Example;
import com.example.nt118_englishvocabapp.models.Pronunciation;
import com.example.nt118_englishvocabapp.models.WordDetail;
import com.example.nt118_englishvocabapp.ui.vocab2.VocabFragment2;
import com.example.nt118_englishvocabapp.ui.vocab4.VocabFragment4;
import com.example.nt118_englishvocabapp.ui.vocab5.VocabFragment5;

import java.io.IOException;

public class VocabFragment3 extends Fragment {

    // ❗️ SỬA LỖI 1: Định nghĩa hằng số ARG_WORD_ID
    public static final String ARG_WORD_ID = "arg_word_id";

    // ❗️ SỬA LỖI 2: Thêm biến TAG
    private static final String TAG = "VocabFragment3";

    private FragmentVocab3Binding binding;
    private VocabWordViewModel viewModel;
    private MediaPlayer mediaPlayer;
    private int wordId = -1;

    public VocabFragment3() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentVocab3Binding.inflate(inflater, container, false);
        // ❗️ SỬA LỖI 3: Không cần biến 'root'

        // 1. Lấy ViewModel (shared)
        viewModel = new ViewModelProvider(requireActivity()).get(VocabWordViewModel.class);

        // 2. Lấy wordId từ VocabFragment2
        Bundle args = getArguments();
        if (args != null) {
            wordId = args.getInt(ARG_WORD_ID, -1);
        }

        // 3. GỌI API ĐỂ LẤY DỮ LIỆU
        if (wordId != -1) {
            viewModel.fetchWordDetails(wordId);
        } else {
            Toast.makeText(getContext(), "Error: No Word ID found", Toast.LENGTH_SHORT).show();
        }

        // 4. THEO DÕI (OBSERVE) DỮ LIỆU
        observeViewModel();

        // 5. CÀI ĐẶT CÁC TAB VÀ NÚT BẤM

        // ❗️ SỬA LỖI 4: Định nghĩa 'hostId'
        final int hostId = (container != null) ? container.getId() : android.R.id.content;

        setupTabs(hostId);
        setupButtons();

        // Khởi tạo tab
        setSelectedTab(binding.tabDefinition);

        Toast.makeText(getContext(), "Vocab Fragment 3 Opened!", Toast.LENGTH_SHORT).show();
        return binding.getRoot(); // 👈 Trả về binding.getRoot()
    }

    /**
     * Theo dõi LiveData từ ViewModel và cập nhật UI
     */
    private void observeViewModel() {
        viewModel.getWordDetail().observe(getViewLifecycleOwner(), wordDetail -> {
            if (wordDetail != null) {
                updateUi(wordDetail);
            } else {
                // Đang tải...
                binding.wordText.setText("Loading...");
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // TODO: Hiển thị/ẩn ProgressBar
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error: " + error);
            }
        });
    }

    /**
     * Điền dữ liệu chi tiết vào các View trong tab Definition
     */
    private void updateUi(WordDetail wordDetail) {
        binding.wordText.setText(wordDetail.getWordText());

        if (wordDetail.getDefinitions() != null && !wordDetail.getDefinitions().isEmpty()) {
            Definition firstDef = wordDetail.getDefinitions().get(0);

            if (firstDef.getPos() != null) {
                binding.wordType.setText("(" + firstDef.getPos().getPosName() + ")");
            }

            binding.definitionVi.setText(firstDef.getTranslationText());
            binding.definitionEn.setText(firstDef.getDefinitionText());

            if (firstDef.getExamples() != null && !firstDef.getExamples().isEmpty()) {
                Example firstEx = firstDef.getExamples().get(0);
                binding.example1En.setText(firstEx.getExampleSentence());
                binding.example1Vi.setText(firstEx.getTranslationSentence());
            }
        }

        Pronunciation ukPron = null;
        Pronunciation usPron = null;
        if (wordDetail.getPronunciations() != null) {
            for (Pronunciation p : wordDetail.getPronunciations()) {
                if ("UK".equals(p.getRegion())) ukPron = p;
                if ("US".equals(p.getRegion())) usPron = p;
            }
        }

        // ❗️ BẮT ĐẦU SỬA LỖI

        // Cập nhật UK
        if (ukPron != null) {
            // 1. Tạo một biến 'final' (hằng số) từ biến 'ukPron'
            final Pronunciation finalUkPron = ukPron;
            binding.ukPron.setText(finalUkPron.getPhoneticSpelling());

            // 2. Dùng biến 'final' đó trong lambda
            binding.playUk.setOnClickListener(v -> playAudio(finalUkPron.getAudioFileUrl()));
        }

        // Cập nhật US
        if (usPron != null) {
            // 1. Tạo một biến 'final'
            final Pronunciation finalUsPron = usPron;
            binding.usPron.setText(finalUsPron.getPhoneticSpelling());

            // 2. Dùng biến 'final' đó trong lambda
            binding.playUs.setOnClickListener(v -> playAudio(finalUsPron.getAudioFileUrl()));
        }

        // ❗️ KẾT THÚC SỬA LỖI

        // TODO: Cập nhật nút Bookmark (Ngôi sao)
        // ...
    }

    /**
     * Cài đặt logic cho 3 Tab
     */
    // ❗️ SỬA LỖI 5: Xóa tham số 'root' không dùng
    private void setupTabs(int hostId) {
        View.OnClickListener tabClick = v -> {
            Bundle b = new Bundle();
            b.putInt(ARG_WORD_ID, wordId); // 👈 Truyền ID

            if (v.getId() == R.id.tab_definition) {
                // Đang ở tab này, không làm gì
                return;
            } else if (v.getId() == R.id.tab_forms) {
                VocabFragment4 f = new VocabFragment4();
                f.setArguments(b);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(hostId, f)
                        .addToBackStack(null)
                        .commit();
            } else if (v.getId() == R.id.tab_synonyms) {
                VocabFragment5 f = new VocabFragment5();
                f.setArguments(b);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(hostId, f)
                        .addToBackStack(null)
                        .commit();
            }
        };

        binding.tabDefinition.setOnClickListener(tabClick);
        binding.tabForms.setOnClickListener(tabClick);
        binding.tabSynonyms.setOnClickListener(tabClick);
    }

    /**
     * Cài đặt logic cho các nút bấm khác (Return, Bookmark)
     */
    // ❗️ SỬA LỖI 6: Xóa tham số 'root' không dùng
    private void setupButtons() {
        binding.btnReturn.setOnClickListener(v -> {
            // Đơn giản hóa logic quay lại
            getParentFragmentManager().popBackStack();
        });

        // TODO: Gán listener cho nút bookmark của bạn
        // binding.btnBookmark.setOnClickListener(v -> {
        //     viewModel.toggleBookmark();
        // });
    }

    // Toggle tab UI (Giữ nguyên)
    private void setSelectedTab(View selected) {
        boolean isDef = selected.getId() == R.id.tab_definition;
        binding.tabDefinition.setSelected(isDef);
        binding.contentDefinition.setVisibility(isDef ? View.VISIBLE : View.GONE);

        binding.tabForms.setSelected(false);
        binding.tabSynonyms.setSelected(false);

        // ❗️ SỬA LỖI 7 (Logic): Ẩn các content khác
        binding.contentForms.setVisibility(View.GONE);
        binding.contentSynonyms.setVisibility(View.GONE);
    }

    // Hàm phát âm thanh
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
                Log.e(TAG, "MediaPlayer error: " + what); // 👈 Dùng TAG
                return true;
            });
        } catch (IOException e) {
            Log.e(TAG, "MediaPlayer setDataSource error", e); // 👈 Dùng TAG
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        binding = null;
    }
}