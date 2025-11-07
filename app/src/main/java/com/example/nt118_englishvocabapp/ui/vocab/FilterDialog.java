package com.example.nt118_englishvocabapp.ui.vocab;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.example.nt118_englishvocabapp.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class FilterDialog extends BottomSheetDialogFragment {

    public FilterDialog() {
        // empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_vocab_filter, container, false);

        ImageButton close = root.findViewById(R.id.close_filter1);
        MaterialButton btnClear = root.findViewById(R.id.btn_clear_filter1);
        MaterialButton btnApply = root.findViewById(R.id.btn_apply_filter1);
        SwitchCompat switchSaved = root.findViewById(R.id.switch_saved_1);
        ChipGroup chipGroup = root.findViewById(R.id.chip_group_difficulty);

        if (close != null) {
            close.setOnClickListener(v -> dismiss());
        }

        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                if (switchSaved != null) switchSaved.setChecked(false);
                if (chipGroup != null) chipGroup.clearCheck();
                // refresh visuals
                refreshChipColors(chipGroup);
                Toast.makeText(getContext(), "Filters cleared", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                // collect filter data
                boolean savedOnly = switchSaved != null && switchSaved.isChecked();
                String difficulty = null;
                if (chipGroup != null) {
                    int checkedId = chipGroup.getCheckedChipId();
                    if (checkedId == R.id.chip_easy) difficulty = "Easy";
                    else if (checkedId == R.id.chip_medium) difficulty = "Medium";
                    else if (checkedId == R.id.chip_hard) difficulty = "Hard";
                }

                // send results back to the owning fragment via FragmentResult
                android.os.Bundle result = new android.os.Bundle();
                result.putBoolean("savedOnly", savedOnly);
                result.putString("difficulty", difficulty);
                // use parent fragment manager so listener on VocabFragment will receive it
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().setFragmentResult("vocabFilter", result);
                }

                Toast.makeText(getContext(), "Filter applied", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }

        // Setup chip visuals so selection shows as colored
        if (chipGroup != null) {
            chipGroup.setOnCheckedChangeListener((group, checkedId) -> refreshChipColors(group));
            // initial refresh to match any pre-checked chip
            refreshChipColors(chipGroup);
        }

        return root;
    }

    private void refreshChipColors(ChipGroup group) {
        if (group == null) return;
        int checkedId = group.getCheckedChipId();

        int colorChecked = ContextCompat.getColor(requireContext(), R.color.purple_700);
        int colorUncheckedBg = Color.parseColor("#F2F2F7");
        int colorCheckedText = ContextCompat.getColor(requireContext(), R.color.white);
        int colorUncheckedText = Color.parseColor("#8a8a8a");

        for (int i = 0; i < group.getChildCount(); i++) {
            View c = group.getChildAt(i);
            if (c instanceof Chip) {
                Chip chip = (Chip) c;
                if (chip.getId() == checkedId) {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(colorChecked));
                    chip.setTextColor(colorCheckedText);
                } else {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(colorUncheckedBg));
                    chip.setTextColor(colorUncheckedText);
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d != null) {
            // Make the bottom sheet container itself transparent so only the internal card shows its white background
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
            }
            if (d.getWindow() != null) {
                d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }
}
