package com.example.nt118_englishvocabapp.ui.account;

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

import com.example.nt118_englishvocabapp.util.ReturnButtonHelper;
import com.example.nt118_englishvocabapp.util.KeyboardUtils;
import com.example.nt118_englishvocabapp.ui.home.HomeFragment;

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
        final android.widget.EditText edtEmail = view.findViewById(com.example.nt118_englishvocabapp.R.id.expanded_email);
        final ImageButton btnPasswordToggle = view.findViewById(com.example.nt118_englishvocabapp.R.id.expanded_password_toggle);

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

        // Apply button: save edits into header and collapse card
        View btnApply = view.findViewById(com.example.nt118_englishvocabapp.R.id.btn_apply_edit);
        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                try {
                    if (!isAdded()) return;

                    // hide keyboard first
                    KeyboardUtils.hideKeyboardAndRestoreUI(requireActivity(), v, keyboardRootView, null);

                    // read values
                    String newName = (edtName != null && edtName.getText() != null) ? edtName.getText().toString().trim() : null;
                    String newEmail = (edtEmail != null && edtEmail.getText() != null) ? edtEmail.getText().toString().trim() : null;

                    if (newName != null && !newName.isEmpty()) {
                        TextView headerName = view.findViewById(com.example.nt118_englishvocabapp.R.id.tv_name);
                        if (headerName != null) headerName.setText(newName);
                    }
                    if (newEmail != null && !newEmail.isEmpty()) {
                        TextView headerEmail = view.findViewById(com.example.nt118_englishvocabapp.R.id.tv_email);
                        if (headerEmail != null) headerEmail.setText(newEmail);
                    }

                    // collapse expanded card and rotate chevron back
                    if (expandedCard != null) expandedCard.setVisibility(View.GONE);
                    if (chevron != null) chevron.animate().rotation(0f).setDuration(200).start();

                    Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
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
    }
}
