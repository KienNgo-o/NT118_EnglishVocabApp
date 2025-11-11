package com.example.nt118_englishvocabapp.ui.auth;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.nt118_englishvocabapp.R;

public class OtpDialogFragment extends DialogFragment {

    public interface OtpListener { void onOtpConfirmed(); }
    private OtpListener listener;
    public void setListener(OtpListener l) { listener = l; }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_otp, container, false);

        EditText[] otps = new EditText[] {
                v.findViewById(R.id.otp_1),
                v.findViewById(R.id.otp_2),
                v.findViewById(R.id.otp_3),
                v.findViewById(R.id.otp_4),
                v.findViewById(R.id.otp_5),
                v.findViewById(R.id.otp_6)
        };

        // Đảm bảo mỗi EditText chỉ nhận 1 ký tự số
        for (EditText e : otps) {
            e.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(1) });
            e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        }

        // Gắn watcher và key listener
        for (int i = 0; i < otps.length; i++) {
            final int index = i;
            final EditText current = otps[index];

            current.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    if (s.length() > 1) {
                        // Chỉ giữ ký tự đầu tiên nếu dán nhiều ký tự
                        current.setText(String.valueOf(s.charAt(0)));
                        current.setSelection(1);
                        return;
                    }
                    if (s.length() == 1) {
                        // Di chuyển đến ô tiếp theo
                        if (index + 1 < otps.length) {
                            otps[index + 1].requestFocus();
                            otps[index + 1].setSelection(otps[index + 1].getText().length());
                        } else {
                            // Ẩn bàn phím nếu đã nhập xong
                            InputMethodManager imm = (InputMethodManager) requireContext()
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
                        }
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            // Xử lý phím xóa
            current.setOnKeyListener((view, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_DEL) {
                    if (current.getText().length() == 0 && index > 0) {
                        otps[index - 1].requestFocus();
                        otps[index - 1].setSelection(otps[index - 1].getText().length());
                        return true;
                    }
                }
                return false;
            });
        }

        Button confirm = v.findViewById(R.id.button_confirm_otp);
        confirm.setOnClickListener(view -> {
            if (listener != null) listener.onOtpConfirmed();
            dismiss();
        });

        // Yêu cầu focus và hiển thị bàn phím
        otps[0].post(() -> {
            otps[0].requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(otps[0], InputMethodManager.SHOW_IMPLICIT);
        });

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Đặt nền trong suốt cho dialog
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}
