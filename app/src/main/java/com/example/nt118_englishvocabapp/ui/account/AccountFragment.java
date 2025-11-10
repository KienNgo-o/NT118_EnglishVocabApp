package com.example.nt118_englishvocabapp.ui.account;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResultCallback;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.SharedPreferences;
import android.content.Context;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.models.User;
import com.example.nt118_englishvocabapp.network.DeleteAccountRequest;
import com.example.nt118_englishvocabapp.network.UpdateProfileRequest;
import com.example.nt118_englishvocabapp.util.ReturnButtonHelper;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;
import com.example.nt118_englishvocabapp.ui.home.HomeFragment;

import com.example.nt118_englishvocabapp.LoginActivity; // Import LoginActivity
import com.example.nt118_englishvocabapp.network.ApiService;
import com.example.nt118_englishvocabapp.network.RetrofitClient;
import com.example.nt118_englishvocabapp.network.SessionManager;
import com.example.nt118_englishvocabapp.network.SignOutRequest;

// Imports cho Retrofit
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class AccountFragment extends Fragment {

    // Activity result launcher for picking an image from gallery
    private ActivityResultLauncher<String> pickImageLauncher;
    private ImageView imgAvatar; // will be assigned in onViewCreated
    private View keyboardRootView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register the GetContent launcher to pick images from the device
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            if (imgAvatar != null) {
                                imgAvatar.setImageURI(uri);
                            } else {
                                // fallback: try to get the view and set it
                                View root = getView();
                                if (root != null) {
                                    ImageView iv = root.findViewById(com.example.nt118_englishvocabapp.R.id.img_avatar);
                                    if (iv != null) iv.setImageURI(uri);
                                }
                            }
                        } else {
                            // user cancelled or no image selected
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "No image selected", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the existing fragment_account layout
        return inflater.inflate(com.example.nt118_englishvocabapp.R.layout.fragment_account, container, false);
    }

    // --- THÊM MỚI CÁC BIẾN CHO LOGOUT ---
    private ApiService apiService;
    private SessionManager sessionManager;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() != null) {
            apiService = RetrofitClient.getApiService(getContext());
            sessionManager = SessionManager.getInstance(getContext());
        }
        // Find views
        LinearLayout rowEdit = view.findViewById(com.example.nt118_englishvocabapp.R.id.row_edit_profile);
        final CardView expandedCard = view.findViewById(com.example.nt118_englishvocabapp.R.id.card_edit_profile_expanded);
        final ImageView chevron = view.findViewById(com.example.nt118_englishvocabapp.R.id.iv_edit_chevron);
        final View rowsContainer = view.findViewById(com.example.nt118_englishvocabapp.R.id.rows_container);

        final TextView rowLabel = view.findViewById(com.example.nt118_englishvocabapp.R.id.tv_row_edit_label);
        final TextView expandedTitle = view.findViewById(com.example.nt118_englishvocabapp.R.id.expanded_card_title);

        // Notifications views
        LinearLayout rowNotifications = view.findViewById(com.example.nt118_englishvocabapp.R.id.row_notifications);
        final CardView notificationsCard = view.findViewById(com.example.nt118_englishvocabapp.R.id.card_notifications_expanded);
        final ImageView notificationsChevron = view.findViewById(com.example.nt118_englishvocabapp.R.id.iv_notifications_chevron);
        final SwitchCompat switchNotifications = view.findViewById(com.example.nt118_englishvocabapp.R.id.switch_notifications_toggle);
        final RadioGroup rgFrequency = view.findViewById(com.example.nt118_englishvocabapp.R.id.rg_notifications_frequency);
        final RadioButton rbEveryday = view.findViewById(com.example.nt118_englishvocabapp.R.id.rb_everyday);
        final RadioButton rbEvery2Days = view.findViewById(com.example.nt118_englishvocabapp.R.id.rb_every_2_days);

        // Dark mode switch
        final SwitchCompat switchDarkMode = view.findViewById(com.example.nt118_englishvocabapp.R.id.switch_dark_mode);

        LinearLayout rowLogout = view.findViewById(com.example.nt118_englishvocabapp.R.id.row_logout);
        LinearLayout rowDeleteAccount = view.findViewById(com.example.nt118_englishvocabapp.R.id.row_delete_account);
        // avatar views
        imgAvatar = view.findViewById(com.example.nt118_englishvocabapp.R.id.img_avatar);
        ImageButton btnAvatarAction = view.findViewById(com.example.nt118_englishvocabapp.R.id.btn_avatar_action);

        // Use Activity content view as stable root for keyboard detection (same pattern as other fragments)
        if (getActivity() != null) {
            keyboardRootView = requireActivity().findViewById(android.R.id.content);
        } else {
            keyboardRootView = view; // fallback
        }

        // initialize enabled state of frequency controls based on the switch's current value
        if (switchNotifications != null) {
            boolean enabled = switchNotifications.isChecked();
            if (rgFrequency != null) rgFrequency.setEnabled(enabled);
            if (rbEveryday != null) rbEveryday.setEnabled(enabled);
            if (rbEvery2Days != null) rbEvery2Days.setEnabled(enabled);
        }

        // Initialize dark mode switch state from SharedPreferences
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            boolean darkEnabled = prefs.getBoolean("pref_dark_mode", false);
            if (switchDarkMode != null) {
                // set checked state before attaching listener to avoid immediate callback
                switchDarkMode.setChecked(darkEnabled);

                switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    try {
                        // persist choice
                        prefs.edit().putBoolean("pref_dark_mode", isChecked).apply();

                        // apply mode
                        if (isChecked) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        }

                        // recreate activity to apply theme across UI
                        if (getActivity() != null) {
                            getActivity().recreate();
                        }
                    } catch (Exception e) {
                        Log.e("AccountFragment", "Error toggling dark mode", e);
                    }
                });
            }
        } catch (Exception e) {
            Log.e("AccountFragment", "Failed to read or write dark mode preference", e);
        }

        // copy initial text
        if (rowLabel != null && expandedTitle != null) {
            expandedTitle.setText(rowLabel.getText());
        }

        if (rowEdit != null && expandedCard != null && chevron != null && rowsContainer != null) {
            rowEdit.setOnClickListener(new View.OnClickListener() {
                private boolean expanded = false;

                @Override
                public void onClick(View v) {
                    // sync title in case label changed
                    if (rowLabel != null && expandedTitle != null) {
                        expandedTitle.setText(rowLabel.getText());
                    }

                    // animate layout changes
                    TransitionManager.beginDelayedTransition((ViewGroup) rowsContainer, new AutoTransition());
                    expanded = !expanded;
                    expandedCard.setVisibility(expanded ? View.VISIBLE : View.GONE);

                    // Do NOT hide the original row label — keep it visible (user requested)
                    // if (rowLabel != null) {
                    //     rowLabel.setVisibility(expanded ? View.INVISIBLE : View.VISIBLE);
                    // }

                    // rotate chevron
                    chevron.animate().rotation(expanded ? 90f : 0f).setDuration(220).start();
                }
            });
        }

        // Wire notifications row similar to edit profile
        if (rowNotifications != null && notificationsCard != null && notificationsChevron != null && rowsContainer != null) {
            rowNotifications.setOnClickListener(new View.OnClickListener() {
                private boolean expanded = false;

                @Override
                public void onClick(View v) {
                    TransitionManager.beginDelayedTransition((ViewGroup) rowsContainer, new AutoTransition());
                    expanded = !expanded;
                    notificationsCard.setVisibility(expanded ? View.VISIBLE : View.GONE);
                    notificationsChevron.animate().rotation(expanded ? 90f : 0f).setDuration(220).start();
                }
            });
        }

        // Avatar button: open gallery to pick an image
        if (btnAvatarAction != null) {
            btnAvatarAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Use the registered ActivityResultLauncher to open the gallery for images
                    if (pickImageLauncher != null) {
                        pickImageLauncher.launch("image/*");
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Cannot open image picker", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }

        // Password toggle: make the expanded edit-card editable and allow toggling visibility
        final android.widget.EditText edtPassword = view.findViewById(com.example.nt118_englishvocabapp.R.id.expanded_password);
        final android.widget.EditText edtName = view.findViewById(com.example.nt118_englishvocabapp.R.id.expanded_username);
        final android.widget.ImageButton btnPasswordToggle = view.findViewById(com.example.nt118_englishvocabapp.R.id.expanded_password_toggle);

        // NEW: old password field and toggle
        final android.widget.EditText edtOldPassword = view.findViewById(com.example.nt118_englishvocabapp.R.id.expanded_old_password);
        final android.widget.ImageButton btnOldPasswordToggle = view.findViewById(com.example.nt118_englishvocabapp.R.id.expanded_old_password_toggle);

        if (btnPasswordToggle != null && edtPassword != null) {
            // start with password hidden (inputType already set in layout)
            btnPasswordToggle.setOnClickListener(v -> {
                try {
                    int inputType = edtPassword.getInputType();
                    // Check whether password is currently visible: if inputType contains TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    boolean isVisible = (inputType & android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
                    if (isVisible) {
                        // hide password
                        edtPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        btnPasswordToggle.setImageResource(com.example.nt118_englishvocabapp.R.drawable.eye_closed);
                    } else {
                        // show password
                        edtPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        btnPasswordToggle.setImageResource(com.example.nt118_englishvocabapp.R.drawable.eye_open);
                    }
                    // Move cursor to end after changing inputType
                    edtPassword.setSelection(edtPassword.getText() != null ? edtPassword.getText().length() : 0);
                } catch (Exception e) {
                    Log.e("AccountFragment", "Error toggling password visibility", e);
                }
            });
        }

        // Wire the old-password toggle similarly
        if (btnOldPasswordToggle != null && edtOldPassword != null) {
            btnOldPasswordToggle.setOnClickListener(v -> {
                try {
                    int inputType = edtOldPassword.getInputType();
                    boolean isVisible = (inputType & android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
                    if (isVisible) {
                        edtOldPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        btnOldPasswordToggle.setImageResource(com.example.nt118_englishvocabapp.R.drawable.eye_closed);
                    } else {
                        edtOldPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        btnOldPasswordToggle.setImageResource(com.example.nt118_englishvocabapp.R.drawable.eye_open);
                    }
                    edtOldPassword.setSelection(edtOldPassword.getText() != null ? edtOldPassword.getText().length() : 0);
                } catch (Exception e) {
                    Log.e("AccountFragment", "Error toggling old password visibility", e);
                }
            });
        }

        // Apply button: save edits into header and collapse card
        View btnApply = view.findViewById(com.example.nt118_englishvocabapp.R.id.btn_apply_edit);
        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                try {
                    if (!isAdded() || getContext() == null) return;

                    // 1. Đọc dữ liệu
                    String currentPass = (edtOldPassword != null && edtOldPassword.getText() != null) ? edtOldPassword.getText().toString() : "";
                    String newName = (edtName != null && edtName.getText() != null) ? edtName.getText().toString().trim() : null;
                    String newPass = (edtPassword != null && edtPassword.getText() != null) ? edtPassword.getText().toString() : null;

                    // 2. Kiểm tra bắt buộc
                    if (currentPass.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập mật khẩu hiện tại", Toast.LENGTH_SHORT).show();
                        edtOldPassword.requestFocus();
                        return;
                    }

                    // 3. Kiểm tra xem có gì để update không
                    boolean isNameChanging = newName != null && !newName.isEmpty();
                    boolean isPassChanging = newPass != null && !newPass.isEmpty();

                    if (!isNameChanging && !isPassChanging) {
                        Toast.makeText(getContext(), "Không có thông tin mới để cập nhật", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // (Hiển thị loading, ví dụ: progressBar.setVisibility(View.VISIBLE))

                    // 4. Tạo Request và Gọi API
                    UpdateProfileRequest request = new UpdateProfileRequest(currentPass, newName, newPass);

                    if (apiService == null) {
                        apiService = RetrofitClient.getApiService(getContext());
                    }

                    apiService.updateUserProfile(request).enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                            // (Ẩn loading)
                            if (!isAdded()) return;

                            if (response.isSuccessful() && response.body() != null) {
                                // THÀNH CÔNG
                                User updatedUser = response.body();
                                Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show();

                                // Cập nhật lại UI (ví dụ: tên ở header)
                                TextView headerName = view.findViewById(R.id.tv_name);
                                if (headerName != null) headerName.setText(updatedUser.getUsername());

                                // Xoá trắng các ô password
                                edtOldPassword.setText("");
                                edtPassword.setText("");

                                // Đóng card (logic cũ của bạn)
                                KeyboardUtils.hideKeyboardAndRestoreUI(requireActivity(), v, keyboardRootView, null);
                                if (expandedCard != null) expandedCard.setVisibility(View.GONE);
                                if (chevron != null) chevron.animate().rotation(0f).setDuration(200).start();

                            } else if (response.code() == 401) {
                                Toast.makeText(getContext(), "Mật khẩu hiện tại không đúng", Toast.LENGTH_LONG).show();
                            } else if (response.code() == 409) {
                                Toast.makeText(getContext(), "Username này đã tồn tại", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getContext(), "Cập nhật thất bại", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                            // (Ẩn loading)
                            if (!isAdded()) return;
                            Log.e("UpdateProfile", "onFailure: " + t.getMessage());
                            Toast.makeText(getContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (Exception e) {
                    Log.e("AccountFragment", "Error applying profile edits", e);
                    Toast.makeText(getContext(), "Failed to apply changes", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Standard return behavior: hide keyboard first, then pop backstack or navigate home as fallback
        View.OnClickListener preClick = v -> {
            if (!isAdded()) return;
            // hide keyboard and restore UI; we don't have a keyboardListener in this fragment so just call the helper
            KeyboardUtils.hideKeyboardAndRestoreUI(
                    requireActivity(),
                    v,
                    keyboardRootView,
                    null
            );
        };

        Runnable fallback = () -> {
            if (!isAdded()) return;
            AppCompatActivity activity = (AppCompatActivity) requireActivity();
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(com.example.nt118_englishvocabapp.R.id.frame_layout, new HomeFragment())
                    .commitAllowingStateLoss();
        };

        ReturnButtonHelper.bind(view, this, preClick, fallback);

        // Listeners for switch and frequency
        if (switchNotifications != null) {
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // For now we just log and enable/disable the frequency controls
                Log.d("AccountFragment", "Notifications enabled: " + isChecked);
                if (rgFrequency != null) rgFrequency.setEnabled(isChecked);
                rbEveryday.setEnabled(isChecked);
                rbEvery2Days.setEnabled(isChecked);
                // TODO: persist this setting (SharedPreferences) and schedule/cancel alarms/notifications
            });
        }

        if (rgFrequency != null) {
            rgFrequency.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    String freq = "everyday";
                    if (checkedId == com.example.nt118_englishvocabapp.R.id.rb_every_2_days) freq = "every_2_days";
                    Log.d("AccountFragment", "Notification frequency: " + freq);
                    // TODO: persist frequency and reschedule notifications accordingly
                }
            });
        }

        if (rowLogout != null) {
            rowLogout.setOnClickListener(v -> {
                if (!isAdded()) return;
                new LogoutDialogFragment().show(getParentFragmentManager(), "logout_dialog");
            });


        }
        if (isAdded()) {
            getParentFragmentManager().setFragmentResultListener(
                    LogoutDialogFragment.REQUEST_KEY, // Key phải khớp với Dialog
                    this,
                    (requestKey, bundle) -> {
                        // Khi "Confirm" được nhấn, hàm performLogout() sẽ chạy
                        performLogout();
                    }
            );
        }

        // 2. GỌI DIALOG: Thiết lập click listener để MỞ dialog
        if (rowLogout != null) {
            rowLogout.setOnClickListener(v -> {
                if (!isAdded()) return;
                // Dòng này mới là dòng MỞ dialog
                new LogoutDialogFragment().show(getParentFragmentManager(), "logout_dialog");
            });
        }
        if (isAdded()) {
            // Lắng nghe kết quả từ DeleteAccDialogFragment
            // "delete_account_confirm" phải khớp với key bạn đặt trong dialog
            getParentFragmentManager().setFragmentResultListener(
                    "delete_account_confirm",
                    this,
                    (requestKey, bundle) -> {
                        // Lấy mật khẩu user vừa nhập từ dialog
                        String password = bundle.getString("password");
                        if (password != null && !password.isEmpty()) {
                            // Gọi hàm thực hiện xoá
                            performDeleteAccount(password);
                        } else {
                            Toast.makeText(getContext(), "Không nhận được mật khẩu", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }


        if (rowDeleteAccount != null) {
            rowDeleteAccount.setOnClickListener(v -> {
                if (!isAdded()) return;
                new DeleteAccDialogFragment().show(getParentFragmentManager(), "deleteacc_dialog");
            });
        }
    }

    private void performLogout() {
        if (!isAdded() || getContext() == null) return; // Kiểm tra an toàn
        if (sessionManager == null) {
            sessionManager = SessionManager.getInstance(getContext());
        }
        String refreshToken = sessionManager.getRefreshToken();
        if (refreshToken == null) {
            // Nếu không có token, không cần gọi API, chỉ cần chuyển màn hình
            forceLogoutToLoginScreen();
            return;
        }

        // Đảm bảo apiService đã được khởi tạo
        if (apiService == null) {
            apiService = RetrofitClient.getApiService(getContext());
        }

        // Gọi API /signout
        apiService.signOut(new SignOutRequest(refreshToken)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                // Dù API thành công hay thất bại, chúng ta CŨNG PHẢI xoá token
                forceLogoutToLoginScreen();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // Lỗi mạng, chúng ta CŨNG PHẢI xoá token
                forceLogoutToLoginScreen();
            }
        });
    }

    /**
     * Hàm này dọn dẹp session và chuyển về màn hình Login
     */
    private void forceLogoutToLoginScreen() {
        if (getActivity() == null || !isAdded()) return; // Đảm bảo Fragment còn tồn tại

        // 1. Xoá token đã lưu
        if (sessionManager == null) {
            sessionManager = SessionManager.getInstance(getContext());
        }
        sessionManager.clearTokens();

        // 2. Chuyển về LoginActivity và xoá hết các Activity cũ
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Kết thúc MainActivity (nơi chứa AccountFragment)
        getActivity().finish();
    }
    private void performDeleteAccount(String password) {
        if (!isAdded() || getContext() == null) return; // Kiểm tra an toàn

        // Tìm ProgressBar (giả sử bạn đã thêm nó vào layout cho chức năng logout)
        // LƯU Ý: Đổi R.id.progress_bar_account nếu bạn dùng ID khác


        // Đảm bảo các service đã được khởi tạo
        if (sessionManager == null) {
            sessionManager = SessionManager.getInstance(getContext());
        }
        if (apiService == null) {
            apiService = RetrofitClient.getApiService(getContext());
        }

        // Gọi API /api/users/me với phương thức DELETE
        apiService.deleteAccount(new DeleteAccountRequest(password)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) return; // Kiểm tra fragment còn tồn tại


                if (response.isSuccessful()) {
                    // Xoá thành công (Code 204)
                    Toast.makeText(getContext(), "Xoá tài khoản thành công", Toast.LENGTH_LONG).show();

                    // Dùng lại hàm logout để dọn dẹp và chuyển màn hình
                    forceLogoutToLoginScreen();

                } else if (response.code() == 401) {
                    // Sai mật khẩu (Code 401)
                    Toast.makeText(getContext(), "Mật khẩu không chính xác", Toast.LENGTH_LONG).show();
                } else {
                    // Lỗi khác (400, 500...)
                    Toast.makeText(getContext(), "Đã xảy ra lỗi khi xoá tài khoản", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) return; // Kiểm tra fragment còn tồn tại

                // Lỗi mạng
                Log.e("DeleteAccount", "onFailure: " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
