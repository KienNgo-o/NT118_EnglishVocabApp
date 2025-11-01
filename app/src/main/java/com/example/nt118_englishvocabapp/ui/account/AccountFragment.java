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

public class AccountFragment extends Fragment {

    // Activity result launcher for picking an image from gallery
    private ActivityResultLauncher<String> pickImageLauncher;
    private ImageView imgAvatar; // will be assigned in onViewCreated

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

        // avatar views
        imgAvatar = view.findViewById(com.example.nt118_englishvocabapp.R.id.img_avatar);
        ImageButton btnAvatarAction = view.findViewById(com.example.nt118_englishvocabapp.R.id.btn_avatar_action);

        // initialize enabled state of frequency controls based on the switch's current value
        if (switchNotifications != null) {
            boolean enabled = switchNotifications.isChecked();
            if (rgFrequency != null) rgFrequency.setEnabled(enabled);
            if (rbEveryday != null) rbEveryday.setEnabled(enabled);
            if (rbEvery2Days != null) rbEvery2Days.setEnabled(enabled);
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
                    chevron.animate().rotation(expanded ? 180f : 0f).setDuration(220).start();
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
                    notificationsChevron.animate().rotation(expanded ? 180f : 0f).setDuration(220).start();
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
