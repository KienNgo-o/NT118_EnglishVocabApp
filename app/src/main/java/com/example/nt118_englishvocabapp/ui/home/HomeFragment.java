// Thay đổi: tất cả comment tiếng Anh đã được chuyển sang tiếng Việt
package com.example.nt118_englishvocabapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.GridLayout;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.view.animation.OvershootInterpolator;
import android.view.ViewParent;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.nio.charset.StandardCharsets;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.content.ContextCompat;

import com.example.nt118_englishvocabapp.R;
import com.example.nt118_englishvocabapp.databinding.FragmentHomeBinding;
import com.example.nt118_englishvocabapp.ui.account.AccountFragment;
import com.example.nt118_englishvocabapp.util.StreakManager;
import com.example.nt118_englishvocabapp.network.SessionManager;
import com.example.nt118_englishvocabapp.ui.flashcard.FlashcardViewModel;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private StreakManager streakManager;
    private int displayYear;
    private int displayMonth;
    // SharedPreferences để lắng nghe thay đổi streak (cập nhật header ngay lập tức)
    private android.content.SharedPreferences streakPrefs;
    private android.content.SharedPreferences.OnSharedPreferenceChangeListener streakPrefListener;
    // ref to overlay added to decor view so we can remove it early if needed
    private ImageView activeOverlayIv = null;
    // refs to pending animator sets so we can cancel them in cleanup
    private AnimatorSet pendingPostArriveAnim = null;

    private enum DayState { ACTIVE, FREEZE, INACTIVE }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        try {
            binding = FragmentHomeBinding.inflate(inflater, container, false);
            View root = binding.getRoot();

            streakManager = new StreakManager(requireContext());
            // đăng ký prefs listener để cập nhật header khi streak thay đổi
            try {
                streakPrefs = requireContext().getSharedPreferences("streak_prefs", Context.MODE_PRIVATE);
                streakPrefListener = (sharedPreferences, key) -> {
                    if ("streak_dates".equals(key) || "streak_pending_announce".equals(key)) {
                        updateHeaderStreak();
                    }
                };
                streakPrefs.registerOnSharedPreferenceChangeListener(streakPrefListener);
            } catch (Exception ignored) {}

            // cập nhật lần đầu cho header
            try { updateHeaderStreak(); } catch (Exception ignored) {}

            // Tìm các view
            TextView quoteText = root.findViewById(R.id.textDialog);
            TextView quizProgress1 = root.findViewById(R.id.text_quiz_progress);
            TextView quizProgress2 = root.findViewById(R.id.text_quiz_progress2);
            // flash progress view sẽ được truy xuất khi cần
            ImageView avatar = root.findViewById(R.id.image_avatar);
            TextView greetingBig = root.findViewById(R.id.text_greeting_big);

            ImageButton prevMonth = root.findViewById(R.id.button_prev_month);
            ImageButton nextMonth = root.findViewById(R.id.button_next_month);
            TextView monthLabel = root.findViewById(R.id.text_month_label);
            GridLayout calendarGrid = root.findViewById(R.id.calendar_grid);

            // khởi tạo tháng hiển thị thành tháng hiện tại
            Calendar c = Calendar.getInstance();
            displayYear = c.get(Calendar.YEAR);
            displayMonth = c.get(Calendar.MONTH);

            // Điền lời chào: dùng tên tài khoản từ resources nếu không có
            try {
                String accountName = getString(R.string.account_name);
                // Gọi username (lấy username)
                try {
                    SessionManager sm = SessionManager.getInstance(requireContext());
                    String stored = sm.getUsername();
                    if (stored != null && !stored.isEmpty()) accountName = stored;
                } catch (Exception ignored) {}
                if (greetingBig != null) {
                    greetingBig.setText(getString(R.string.greeting_format, accountName));
                }
            } catch (Exception ignored) {
            }

            // Quan sát dữ liệu và cập nhật UI
            homeViewModel.getTodayQuote().observe(getViewLifecycleOwner(), s -> {
                if (quoteText != null) quoteText.setText(s);
            });
            // Quan sát tiến độ flashcard từ shared FlashcardViewModel (if present)
            try {
                FlashcardViewModel flashVm = new ViewModelProvider(requireActivity()).get(FlashcardViewModel.class);
                flashVm.getProgressList().observe(getViewLifecycleOwner(), list -> {
                    TextView flashTopicView = root.findViewById(R.id.text_flash_topic);
                    TextView flashProgressView = root.findViewById(R.id.text_flash_progress);
                    TextView flashTopicView2 = root.findViewById(R.id.text_flash_topic2);
                    TextView flashProgressView2 = root.findViewById(R.id.text_flash_progress2);

                    if (list == null || list.isEmpty()) {
                        if (flashTopicView != null) flashTopicView.setText(getString(R.string.placeholder_topic_title));
                        if (flashProgressView != null) flashProgressView.setText(getString(R.string.progress_0_0));
                        if (flashTopicView2 != null) flashTopicView2.setText(getString(R.string.placeholder_topic_title));
                        if (flashProgressView2 != null) flashProgressView2.setText(getString(R.string.progress_0_0));
                        Log.d("HomeFragment", "progressList empty - set defaults for both rows");
                        return;
                    }

                    // tìm hai mục gần nhất theo timestamp
                    FlashcardViewModel.ProgressItem first = null;
                    FlashcardViewModel.ProgressItem second = null;
                    for (FlashcardViewModel.ProgressItem p : list) {
                        if (p == null) continue;
                        if (first == null || p.timestamp > first.timestamp) {
                            second = first;
                            first = p;
                        } else if (second == null || p.timestamp > second.timestamp) {
                            second = p;
                        }
                    }

                    if (first != null) {
                        Log.d("HomeFragment", "progressList observer - first topicId=" + first.topicId + " name=" + first.topicName + " studied=" + first.studied + " total=" + first.total + " ts=" + first.timestamp);
                        String name = (first.topicName == null || first.topicName.isEmpty()) ? getString(R.string.placeholder_topic_title) : first.topicName;
                        if (flashTopicView != null) flashTopicView.setText(name);
                        if (flashProgressView != null) flashProgressView.setText(first.studied + "/" + first.total + " (" + first.getProgressPercent() + "%)");
                    }
                    if (second != null) {
                        Log.d("HomeFragment", "progressList observer - second topicId=" + second.topicId + " name=" + second.topicName + " studied=" + second.studied + " total=" + second.total + " ts=" + second.timestamp);
                        String name2 = (second.topicName == null || second.topicName.isEmpty()) ? getString(R.string.placeholder_topic_title) : second.topicName;
                        if (flashTopicView2 != null) flashTopicView2.setText(name2);
                        if (flashProgressView2 != null) flashProgressView2.setText(second.studied + "/" + second.total + " (" + second.getProgressPercent() + "%)");
                    } else {
                        // nếu không có mục thứ hai, đặt mặc định
                        if (flashTopicView2 != null) flashTopicView2.setText(getString(R.string.placeholder_topic_title));
                        if (flashProgressView2 != null) flashProgressView2.setText(getString(R.string.progress_0_0));
                    }
                });
            } catch (Exception ignored) {}
            
            homeViewModel.getQuizProgress1().observe(getViewLifecycleOwner(), s -> {
                if (quizProgress1 != null) quizProgress1.setText(s);
            });
            homeViewModel.getQuizProgress2().observe(getViewLifecycleOwner(), s -> {
                if (quizProgress2 != null) quizProgress2.setText(s);
            });
            // Lưu ý: tiến độ flash được điều khiển bởi FlashcardViewModel (local).
            // Tránh ghi đè nó bằng giá trị tĩnh từ HomeViewModel.
            // homeViewModel.getFlashProgress().observe(getViewLifecycleOwner(), s -> {
            //     if (flashProgress != null) flashProgress.setText(s);
            // });

            // điều hướng tháng
            prevMonth.setOnClickListener(v -> {
                displayMonth--;
                if (displayMonth < 0) {
                    displayMonth = 11;
                    displayYear--;
                }
                populateCalendar(calendarGrid, monthLabel);
            });
            nextMonth.setOnClickListener(v -> {
                displayMonth++;
                if (displayMonth > 11) {
                    displayMonth = 0;
                    displayYear++;
                }
                populateCalendar(calendarGrid, monthLabel);
            });

            // khởi tạo lịch lần đầu
            populateCalendar(calendarGrid, monthLabel);

            // Nhấn avatar -> mở AccountFragment
            avatar.setOnClickListener(v -> {
                try {
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.frame_layout, new AccountFragment())
                                .addToBackStack(null)
                                .commit();
                    }
                } catch (Exception ignored) {
                }
            });

            // Rate button: mở dialog đánh giá
            try {
                View rateBtn = root.findViewById(R.id.btn_rate_app);
                if (rateBtn != null) {
                    rateBtn.setOnClickListener(v -> {
                        try {
                            android.view.LayoutInflater li = getLayoutInflater();
                            View dlgView = li.inflate(R.layout.dialog_rate, null, false);
                            android.widget.RatingBar rb = dlgView.findViewById(R.id.rating_bar);
                            android.widget.EditText etComment = dlgView.findViewById(R.id.et_rate_comment);
                            android.widget.Button btnSubmit = dlgView.findViewById(R.id.btn_rate_submit);

                            final android.app.Dialog d = new android.app.Dialog(new android.view.ContextThemeWrapper(requireContext(), R.style.RateDialogTheme));
                            d.setContentView(dlgView);
                            d.setCancelable(true);
                            if (d.getWindow() != null) {
                                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                                // dim the background a bit when the dialog is shown
                                d.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                                d.getWindow().setDimAmount(0.70f); // slightly darker outside

                                // reduce width a little: set dialog width to 86% of screen width
                                android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
                                lp.copyFrom(d.getWindow().getAttributes());
                                int widthPx = (int) (getResources().getDisplayMetrics().widthPixels * 0.86f);
                                lp.width = widthPx;
                                // increase height a bit: set a min height for dialog content
                                lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
                                d.getWindow().setAttributes(lp);
                            }

                            // Make rating interactive and tint stars on change
                            if (rb != null) {
                                // ensure default rating 0 and stars grey by default
                                try {
                                    rb.setRating(0f);
                                    // enlarge stars visually but keep drawable size stable by using scale in layout
                                    android.graphics.drawable.LayerDrawable initStars = (android.graphics.drawable.LayerDrawable) rb.getProgressDrawable();
                                    android.graphics.drawable.Drawable filledInit = initStars.getDrawable(2);
                                    android.graphics.drawable.Drawable halfInit = initStars.getDrawable(1);
                                    android.graphics.drawable.Drawable emptyInit = initStars.getDrawable(0);
                                    // use darker gray so stars are clearly visible
                                    int greyColor = ContextCompat.getColor(requireContext(), R.color.dialog_star_grey);
                                    try { filledInit.setTint(greyColor); } catch (Exception ignored) {}
                                    try { halfInit.setTint(greyColor); } catch (Exception ignored) {}
                                    try { emptyInit.setTint(greyColor); } catch (Exception ignored) {}
                                } catch (Exception ignored) {}

                                rb.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
                                    try {
                                        int selectedColor = ContextCompat.getColor(requireContext(), R.color.orange);
                                        int greyColor = ContextCompat.getColor(requireContext(), R.color.dialog_star_grey);

                                        // We want only the filled portion up to current rating to be colored
                                        android.graphics.drawable.LayerDrawable stars = (android.graphics.drawable.LayerDrawable) ratingBar.getProgressDrawable();
                                        android.graphics.drawable.Drawable filled = stars.getDrawable(2);
                                        android.graphics.drawable.Drawable secondary = stars.getDrawable(1);
                                        android.graphics.drawable.Drawable background = stars.getDrawable(0);

                                        // Tint everything grey first
                                        try { background.setTint(greyColor); } catch (Exception ignored) {}
                                        try { secondary.setTint(greyColor); } catch (Exception ignored) {}
                                        try { filled.setTint(greyColor); } catch (Exception ignored) {}

                                        // Then color the filled part (drawn area) with selected color. The RatingBar will mask it according to rating.
                                        try { filled.setTint(selectedColor); } catch (Exception ignored) {}

                                        // Do not change star size on selection
                                    } catch (Exception ex) {
                                        Log.e("HomeFragment", "rating change handling error", ex);
                                    }
                                });
                            }

                            btnSubmit.setOnClickListener(bv -> {
                                try {
                                    float ratingVal = (rb != null) ? rb.getRating() : 0f;
                                    String comment = (etComment != null) ? etComment.getText().toString() : "";

                                    // send rating to server via Retrofit
                                    try {
                                        com.example.nt118_englishvocabapp.network.ApiService api = com.example.nt118_englishvocabapp.network.RetrofitClient.getApiService(requireContext());
                                        java.util.Map<String, Integer> payload = new java.util.HashMap<>();
                                        payload.put("rating", (int) ratingVal);

                                        api.rateApp(payload).enqueue(new retrofit2.Callback<Void>() {
                                            @Override
                                            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                                                if (response.isSuccessful()) {
                                                    Toast.makeText(requireContext(), getString(R.string.rate_dialog_thanks), Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(requireContext(), getString(R.string.rate_dialog_submit_error), Toast.LENGTH_SHORT).show();
                                                }
                                            }

                                            @Override
                                            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                                                Log.e("HomeFragment", "rateApp request failed", t);
                                                Toast.makeText(requireContext(), getString(R.string.rate_dialog_submit_error), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } catch (Exception ex) {
                                        Log.e("HomeFragment", "error sending rating", ex);
                                    }

                                    d.dismiss();
                                } catch (Exception ex) {
                                    Log.e("HomeFragment", "rate submit error", ex);
                                    try { d.dismiss(); } catch (Exception ignored) {}
                                }
                            });

                            d.show();
                        } catch (Exception ex) {
                            Log.e("HomeFragment", "show rate dialog error", ex);
                        }
                    });
                }
            } catch (Exception ignored) {}

            return root;
        } catch (Exception ex) {
            Log.e("HomeFragment", "onCreateView error", ex);
            try {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Home UI error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            } catch (Exception ignored) {}
            // trả về một View rỗng để tránh gây lỗi
            return new View(requireContext());
        }
    }

    // Cập nhật text_streak_number ở header theo giá trị hiện tại trong StreakManager
    private void updateHeaderStreak() {
        try {
            if (getActivity() == null) return;
            int currentStreak = (streakManager != null) ? streakManager.getCurrentStreak() : 0;
            if (binding != null && binding.textStreakNumber != null) {
                binding.textStreakNumber.setText(String.valueOf(currentStreak));
                return;
            }
            // fallback: tìm view trong fragment root
            View root = getView();
            if (root != null) {
                TextView headerStreak = root.findViewById(R.id.text_streak_number);
                if (headerStreak != null) headerStreak.setText(String.valueOf(currentStreak));
                return;
            }
            // fallback 2: tìm trong activity
            if (getActivity() != null) {
                TextView headerStreak = getActivity().findViewById(R.id.text_streak_number);
                if (headerStreak != null) headerStreak.setText(String.valueOf(currentStreak));
            }
        } catch (Exception ignored) {}
    }

    // Hiển thị dialog chúc mừng streak trước khi chạy animation overlay
    private void showStreakDialog(ImageView flameView, View cellRoot, int streakCount) {
        if (getActivity() == null || flameView == null) return;
        try {
            View dlg = getLayoutInflater().inflate(R.layout.dialog_streak, null, false);
            ImageView iv = dlg.findViewById(R.id.iv_streak_dialog);
            TextView tv = dlg.findViewById(R.id.tv_streak_message);

            // sử dụng drawable hiện tại của flameView cho dialog
            if (flameView.getDrawable() != null) {
                iv.setImageDrawable(flameView.getDrawable());
            }

            // set kích thước lớn như overlay khởi điểm (tương ứng)
            int sizeDp = 160; // lớn hơn so với pill, để khớp cảm giác lớn lúc bắt đầu
            int sizePx = (int) (sizeDp * getResources().getDisplayMetrics().density);
            iv.getLayoutParams().width = sizePx;
            iv.getLayoutParams().height = sizePx;
            iv.requestLayout();

            // text: format từ string resource (Chúc mừng bạn đã có chuỗi %1$d ngày)
            try {
                tv.setText(getString(R.string.streak_dialog_message, streakCount));
            } catch (Exception e) {
                // fallback nếu có vấn đề với resource
                tv.setText("Chúc mừng bạn đã có chuỗi " + streakCount + " ngày");
            }

            final android.app.Dialog d = new android.app.Dialog(requireContext());
            d.setContentView(dlg);
            d.setCancelable(true);
            d.setCanceledOnTouchOutside(true);
            if (d.getWindow() != null) d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            // flag để tránh gọi animation 2 lần khi user tap vào nội dung (dialog.dismiss -> onDismiss)
            final boolean[] animationStarted = {false};

            // khi tap dialog -> đóng dialog và bắt đầu animation
            dlg.setOnClickListener(v -> {
                try {
                    animationStarted[0] = true;
                    d.dismiss();
                } catch (Exception ignored) {}
                try {
                    // bắt đầu animation overlay vào vị trí của flameView
                    animateToday(flameView, cellRoot);
                } catch (Exception ignored) {}
            });

            // nếu người dùng chạm bên ngoài hoặc dialog bị cancel thì cũng chạy animation (nếu chưa chạy)
            d.setOnCancelListener(dialog -> {
                try {
                    if (!animationStarted[0]) {
                        animationStarted[0] = true;
                        animateToday(flameView, cellRoot);
                    }
                } catch (Exception ignored) {}
            });

            d.setOnDismissListener(dialog -> {
                try {
                    if (!animationStarted[0]) {
                        animationStarted[0] = true;
                        animateToday(flameView, cellRoot);
                    }
                } catch (Exception ignored) {}
            });

            d.show();
        } catch (Exception ex) {
            Log.e("HomeFragment", "showStreakDialog error", ex);
        }
    }

    private void populateCalendar(GridLayout grid, TextView monthLabel) {
        if (grid == null || monthLabel == null) return;
        try {
            grid.removeAllViews();
        } catch (Exception ex) {
            Log.e("HomeFragment", "populateCalendar failed", ex);
            try {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Calendar error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            } catch (Exception ignored) {}
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, displayYear);
        cal.set(Calendar.MONTH, displayMonth);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstWeekday = cal.get(Calendar.DAY_OF_WEEK); // Chủ nhật=1 ... Thứ bảy=7
        // chuyển sang chỉ số Thứ hai=0 .. Chủ nhật=6
        int startOffset = (firstWeekday + 5) % 7; // nếu Chủ nhật(1)->6, Thứ hai(2)->0, v.v.

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // hiển thị tên tháng
        String monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        monthLabel.setText(monthName);

        // xây tập các ngày active
        List<Integer> active = streakManager.getActiveDaysForMonth(displayYear, displayMonth);
        Set<Integer> activeSet = new HashSet<>();
        if (active != null && !active.isEmpty()) activeSet.addAll(active);

        // lưới có 7 cột. Tạo 6*7 ô (một số rỗng)
        int totalCells = 6 * 7;
        for (int i = 0; i < totalCells; i++) {
            View cell = getLayoutInflater().inflate(R.layout.item_calendar_day, grid, false);
            TextView tv = cell.findViewById(R.id.text_day_number);
            ImageView flame = cell.findViewById(R.id.bg_flame);
            View cellRoot = cell.findViewById(R.id.day_cell_root);

            // Nếu layout thiếu view mong đợi, bỏ qua cell này
            if (tv == null || flame == null) {
                continue;
            }

            int dayNumber = i - startOffset + 1;
            if (dayNumber >= 1 && dayNumber <= daysInMonth) {
                tv.setText(String.valueOf(dayNumber));
                // Xác định trạng thái ngày
                boolean isActive = activeSet.contains(dayNumber);
                Calendar today = Calendar.getInstance();
                int tYear = today.get(Calendar.YEAR);
                int tMonth = today.get(Calendar.MONTH);
                int tDay = today.get(Calendar.DAY_OF_MONTH);
                boolean isToday = (tYear == displayYear && tMonth == displayMonth && tDay == dayNumber);

                DayState state;
                if (isToday) {
                    // hôm nay có thể là ACTIVE (nếu được đánh dấu) hoặc FREEZE (nếu không được đánh dấu)
                    state = isActive ? DayState.ACTIVE : DayState.FREEZE;
                } else {
                    state = isActive ? DayState.ACTIVE : DayState.INACTIVE;
                }

                // Áp dụng nền và màu chữ theo trạng thái
                if (cellRoot != null) {
                    switch (state) {
                        case ACTIVE:
                            cellRoot.setBackgroundResource(R.drawable.day_peach_pill);
                            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.day_peach_text));
                            break;
                        case FREEZE:
                            cellRoot.setBackgroundResource(R.drawable.day_blue_pill);
                            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.day_blue_text));
                            break;
                        default:
                            cellRoot.setBackgroundResource(R.drawable.day_gray_pill);
                            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.day_gray_text));
                            break;
                    }
                }

                final int resId = (state == DayState.ACTIVE) ? R.drawable.streak : (state == DayState.FREEZE ? R.drawable.streak_freeze : R.drawable.streak_no);
                flame.setVisibility(View.VISIBLE);
                flame.setImageResource(resId);

                // Dịch chuyển hình `streak_no`sang phải 1 xíu để căn nhìn cho thẳng
                // Chuyển 3dp sang pixel để nhất quán trên các mật độ màn hình
                try {
                    float offsetDp = 3f;
                    float offsetPx = getResources().getDisplayMetrics().density * offsetDp;
                    // Shift both the inactive (streak_no) and freeze (streak_freeze) icons slightly to the right
                    if (resId == R.drawable.streak_no || resId == R.drawable.streak_freeze) {
                        flame.setTranslationX(offsetPx);
                    } else {
                        flame.setTranslationX(0f);
                    }
                } catch (Exception ignored) {}

                // Giảm opacity cho icon streak_no (inactive)
                try {
                    if (resId == R.drawable.streak_no) {
                        flame.setAlpha(0.5f); // 50% opacity cho icon inactive
                    } else {
                        flame.setAlpha(1f);
                    }
                } catch (Exception ignored) {}

                // Để layout xử lý căn dọc giữa đáy ngày và đáy pill
                if (isActive && isToday) {
                    // chỉ hiển thị dialog/animation nếu StreakManager báo có pending announcement (user vừa hoàn thành topic hôm nay)
                    try {
                        if (streakManager.hasPendingAnnouncementForToday()) {
                            int currentStreakNow = streakManager.getCurrentStreak();
                            flame.post(() -> {
                                try {
                                    showStreakDialog(flame, cellRoot, currentStreakNow);
                                    // sau khi show dialog, clear pending announce để không show lại
                                    streakManager.clearPendingAnnouncement();
                                } catch (Exception ignored) {
                                }
                            });
                        }
                    } catch (Exception ignored) {}
                }
             } else {
                 // ô trống
                 tv.setText("");
                 flame.setVisibility(View.GONE);
                 if (cellRoot != null) cellRoot.setBackground(null);
             }

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            cell.setLayoutParams(lp);
            grid.addView(cell);
        }

        // cập nhật số streak trên header
        try {
            TextView headerStreak = null;
            View root = grid.getRootView();
            if (root != null) headerStreak = root.findViewById(R.id.text_streak_number);

            if (headerStreak == null && getActivity() != null) {
                headerStreak = getActivity().findViewById(R.id.text_streak_number);
            }
            // tính lại tổng số active days từ StreakManager trước khi cập nhật header
            int currentStreak = streakManager.getCurrentStreak();
            if (headerStreak != null) {
                headerStreak.setText(String.valueOf(currentStreak));
            }
        } catch (Exception ex) {
            Log.e("HomeFragment", "populateCalendar failed", ex);
            try {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Calendar error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            } catch (Exception ignored) {}
        }
    }

    // animation nổi bật cho hôm nay khi active: phồng + nhịp + bounce nhẹ cho container
    private void animateToday(View icon, View container) {
        // Sử dụng một overlay copy của icon để animate từ giữa màn hình vào vị trí thực tế.
        if (getActivity() == null || icon == null) return;
        icon.post(() -> {
            try {
                // lấy drawable từ icon (nếu là ImageView) hoặc background
                android.graphics.drawable.Drawable dr = null;
                if (icon instanceof ImageView) {
                    dr = ((ImageView) icon).getDrawable();
                }
                if (dr == null) dr = icon.getBackground();
                if (dr == null) return; // không có drawable để sao chép

                // overlay parent: decor view của activity (đảm bảo không bị cắt bởi các parent)
                ViewGroup decor = (ViewGroup) getActivity().getWindow().getDecorView();

                // tạo ImageView overlay
                ImageView overlayIv = new ImageView(requireContext());
                overlayIv.setImageDrawable(dr);
                overlayIv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                // lưu ref để cleanup nếu fragment bị destroy sớm
                activeOverlayIv = overlayIv;

                // kích thước overlay: dùng kích thước icon nếu có, nếu không thì một giá trị mặc định
                int w = icon.getWidth() > 0 ? icon.getWidth() : (int) (48 * getResources().getDisplayMetrics().density);
                int h = icon.getHeight() > 0 ? icon.getHeight() : (int) (48 * getResources().getDisplayMetrics().density);
                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(w, h);
                decor.addView(overlayIv, lp);
                overlayIv.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                                  View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));

                // vị trí bắt đầu: tâm màn hình (overlay sẽ xuất hiện lớn ở giữa)
                final android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                float startX = dm.widthPixels / 2f - overlayIv.getMeasuredWidth() / 2f;
                float startY = dm.heightPixels / 2f - overlayIv.getMeasuredHeight() / 2f;

                // KHỞI ĐIỂM SCALE LỚN HƠN
                final float START_SCALE = 10.0f; // <-- tăng từ 6.0f lên 10.0f để icon bắt đầu lớn hơn nhiều

                overlayIv.setX(startX);
                overlayIv.setY(startY);
                overlayIv.setScaleX(START_SCALE);
                overlayIv.setScaleY(START_SCALE);
                overlayIv.setAlpha(0f);
                overlayIv.bringToFront();

                // tính vị trí đích của overlay: căn về tọa độ của icon trên màn hình
                int[] iconLoc = new int[2];
                icon.getLocationOnScreen(iconLoc);
                // căn chính giữa overlay với icon (overlay có thể khác kích thước so với icon)
                float targetX = iconLoc[0] + (icon.getWidth() - overlayIv.getMeasuredWidth()) / 2f;
                float targetY = iconLoc[1] + (icon.getHeight() - overlayIv.getMeasuredHeight()) / 2f;

                // ẩn icon thật trong lúc overlay chạy
                icon.setAlpha(0f);

                // tạo animator: di chuyển, scale và alpha
                ObjectAnimator aX = ObjectAnimator.ofFloat(overlayIv, "x", startX, targetX);
                ObjectAnimator aY = ObjectAnimator.ofFloat(overlayIv, "y", startY, targetY);
                ObjectAnimator sX = ObjectAnimator.ofFloat(overlayIv, "scaleX", START_SCALE, 1.0f);
                ObjectAnimator sY = ObjectAnimator.ofFloat(overlayIv, "scaleY", START_SCALE, 1.0f);
                ObjectAnimator aAlpha = ObjectAnimator.ofFloat(overlayIv, "alpha", 0f, 1f);

                AnimatorSet arrive = new AnimatorSet();
                arrive.playTogether(aX, aY, sX, sY, aAlpha);
                arrive.setInterpolator(new OvershootInterpolator(1.6f));
                // tăng duration để làm hiệu ứng chậm hơn, trông uy lực hơn
                arrive.setDuration(1200);

                // nhịp nhỏ trên icon thật sau khi đến
                ObjectAnimator pulseX = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.08f);
                ObjectAnimator pulseY = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.08f);
                pulseX.setRepeatMode(ValueAnimator.REVERSE);
                pulseY.setRepeatMode(ValueAnimator.REVERSE);
                pulseX.setRepeatCount(1);
                pulseY.setRepeatCount(1);
                // làm nhịp dài hơn một chút
                pulseX.setDuration(400);
                pulseY.setDuration(400);
                AnimatorSet pulse = new AnimatorSet();
                pulse.playTogether(pulseX, pulseY);

                // bounce nhẹ cho pill container
                ObjectAnimator cScaleX = ObjectAnimator.ofFloat(container, "scaleX", 1f, 1.03f);
                ObjectAnimator cScaleY = ObjectAnimator.ofFloat(container, "scaleY", 1f, 1.03f);
                cScaleX.setRepeatMode(ValueAnimator.REVERSE);
                cScaleY.setRepeatMode(ValueAnimator.REVERSE);
                cScaleX.setRepeatCount(1);
                cScaleY.setRepeatCount(1);
                // tăng duration để bounce chậm và mượt hơn
                cScaleX.setDuration(400);
                cScaleY.setDuration(400);
                AnimatorSet containerPulse = new AnimatorSet();
                containerPulse.playTogether(cScaleX, cScaleY);

                // Khi arrive kết thúc: nhanh chóng xóa overlay và hiện icon thật, rồi chạy pulse+bounce trên views thật
                arrive.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        try {
                            // hiển thị icon thật và xóa overlay NGAY LẬP TỨC sau khi arrive hoàn thành
                            icon.setAlpha(1f);
                            if (activeOverlayIv != null) {
                                try {
                                    ViewParent p = activeOverlayIv.getParent();
                                    if (p instanceof ViewGroup) ((ViewGroup) p).removeView(activeOverlayIv);
                                } catch (Exception ignored) {}
                                activeOverlayIv = null;
                            }

                            // bắt đầu pulse và container bounce trên view thật
                            pendingPostArriveAnim = new AnimatorSet();
                            pendingPostArriveAnim.playSequentially(pulse, containerPulse);
                            pendingPostArriveAnim.start();

                            // khi sequence này kết thúc, clear ref
                            pendingPostArriveAnim.addListener(new android.animation.AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(android.animation.Animator animation) {
                                    pendingPostArriveAnim = null;
                                }

                                @Override
                                public void onAnimationCancel(android.animation.Animator animation) {
                                    pendingPostArriveAnim = null;
                                }
                            });
                        } catch (Exception ignored) {}
                    }

                    @Override
                    public void onAnimationCancel(android.animation.Animator animation) {
                        try {
                            // đảm bảo xóa overlay nếu arrive bị hủy
                            icon.setAlpha(1f);
                            if (activeOverlayIv != null) {
                                try {
                                    ViewParent p = activeOverlayIv.getParent();
                                    if (p instanceof ViewGroup) ((ViewGroup) p).removeView(activeOverlayIv);
                                } catch (Exception ignored) {}
                                activeOverlayIv = null;
                            }
                        } catch (Exception ignored) {}
                    }
                });

                // khởi động arrive
                arrive.start();
            } catch (Exception ignored) {
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // cleanup: nếu có overlay chưa bị xóa, remove nó ngay
        try {
            if (activeOverlayIv != null) {
                ViewParent p = activeOverlayIv.getParent();
                if (p instanceof ViewGroup) ((ViewGroup) p).removeView(activeOverlayIv);
                activeOverlayIv = null;
            }
        } catch (Exception ignored) {}
        // cancel any pending post-arrive animations
        try {
            if (pendingPostArriveAnim != null) {
                pendingPostArriveAnim.cancel();
                pendingPostArriveAnim = null;
            }
        } catch (Exception ignored) {}
        // hủy đăng ký prefs listener
        try {
            if (streakPrefs != null && streakPrefListener != null) {
                streakPrefs.unregisterOnSharedPreferenceChangeListener(streakPrefListener);
                streakPrefListener = null;
            }
        } catch (Exception ignored) {}
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Áp dụng tiến trình flashcard đã lưu làm phương án dự phòng nếu LiveData chưa cập nhật
        try {
            applyLatestProgressFromPrefs(binding != null ? binding.getRoot() : null);
        } catch (Exception ignored) {}
        // Áp dụng tiến trình quiz đã lưu nếu có
        try {
            applyLatestQuizProgressFromPrefs(binding != null ? binding.getRoot() : null);
        } catch (Exception ignored) {}
    }

    // Trợ giúp: đọc prefs chia sẻ "flashcard_progress" và chọn tiến trình chủ đề được lưu gần đây nhất
    private void applyLatestProgressFromPrefs(View root) {
        if (getContext() == null || root == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("flashcard_progress", Context.MODE_PRIVATE);
        if (prefs == null) return;
        long firstTs = -1L, secondTs = -1L;
        String firstName = null, secondName = null;
        int firstStudied = 0, firstTotal = 0;
        int secondStudied = 0, secondTotal = 0;

        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith("progress_topic_")) continue;
            Object obj = prefs.getAll().get(key);
            if (!(obj instanceof String)) continue;
            String s = (String) obj;
            String[] parts = s.split("\\|", -1);
            if (parts.length < 4) continue;
            String encodedName = parts[0];
            int studied = 0;
            int total = 0;
            long ts = 0L;
            try { studied = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
            try { total = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
            try { ts = Long.parseLong(parts[3]); } catch (NumberFormatException ignored) {}
            String name = "";
            try { name = new String(Base64.decode(encodedName, Base64.NO_WRAP), StandardCharsets.UTF_8); } catch (Exception ignored) {}

            if (ts > firstTs) {
                // shift down
                secondTs = firstTs; secondName = firstName; secondStudied = firstStudied; secondTotal = firstTotal;
                firstTs = ts; firstName = name; firstStudied = studied; firstTotal = total;
            } else if (ts > secondTs) {
                secondTs = ts; secondName = name; secondStudied = studied; secondTotal = total;
            }
        }

        TextView flashTopicView = root.findViewById(R.id.text_flash_topic);
        TextView flashProgressView = root.findViewById(R.id.text_flash_progress);
        TextView flashTopicView2 = root.findViewById(R.id.text_flash_topic2);
        TextView flashProgressView2 = root.findViewById(R.id.text_flash_progress2);

        if (firstTs < 0) {
            if (flashTopicView != null) flashTopicView.setText(getString(R.string.placeholder_topic_title));
            if (flashProgressView != null) flashProgressView.setText(getString(R.string.progress_0_0));
        } else {
            if (flashTopicView != null) flashTopicView.setText(firstName == null || firstName.isEmpty() ? getString(R.string.placeholder_topic_title) : firstName);
            if (flashProgressView != null) flashProgressView.setText(firstStudied + "/" + firstTotal + " (" + (firstTotal<=0?0:Math.round((firstStudied*100f)/firstTotal)) + "%)");
        }

        if (secondTs < 0) {
            if (flashTopicView2 != null) flashTopicView2.setText(getString(R.string.placeholder_topic_title));
            if (flashProgressView2 != null) flashProgressView2.setText(getString(R.string.progress_0_0));
        } else {
            if (flashTopicView2 != null) flashTopicView2.setText(secondName == null || secondName.isEmpty() ? getString(R.string.placeholder_topic_title) : secondName);
            if (flashProgressView2 != null) flashProgressView2.setText(secondStudied + "/" + secondTotal + " (" + (secondTotal<=0?0:Math.round((secondStudied*100f)/secondTotal)) + "%)");
        }
    }

    // Trợ giúp: Điền card Quiz bằng cùng dữ liệu được lưu (fallback) — dùng format giống flashcard section
    private void applyLatestQuizProgressFromPrefs(View root) {
        if (getContext() == null || root == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("quiz_progress", Context.MODE_PRIVATE);
        if (prefs == null) return;
        // Prefer the explicit "quiz_latest" key if present
        String latest = prefs.getString("quiz_latest", null);
        if (latest != null) {
            // parse and display just the latest entry
            try {
                String[] parts = latest.split("\\|", -1);
                if (parts.length >= 4) {
                    String title = new String(android.util.Base64.decode(parts[0], android.util.Base64.NO_WRAP), java.nio.charset.StandardCharsets.UTF_8);
                    int score = Integer.parseInt(parts[1]);
                    // display
                    TextView quizTopic1 = root.findViewById(R.id.text_quiz_topic);
                    TextView quizProgress1 = root.findViewById(R.id.text_quiz_progress);
                    if (quizTopic1 != null) quizTopic1.setText(title == null || title.isEmpty() ? getString(R.string.placeholder_topic_title) : title);
                    int s = Math.max(0, Math.min(score, 100));
                    if (quizProgress1 != null) quizProgress1.setText(s + "/100 (" + s + "%)");
                }
            } catch (Exception ignored) {}
            // We displayed the latest entry; still allow scanning for two items if needed, but return to avoid double-setting
            return;
        }
        // (reuse 'prefs' declared above) — continue scanning stored quiz_result_topic_* entries
        long firstTs = -1L, secondTs = -1L;
        String firstTitle = null, secondTitle = null;
        int firstScore = 0, firstTotal = 0;
        int secondScore = 0, secondTotal = 0;

        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith("quiz_result_topic_")) continue;
            Object obj = prefs.getAll().get(key);
            if (!(obj instanceof String)) continue;
            String s = (String) obj;
            String[] parts = s.split("\\|", -1);
            if (parts.length < 4) continue;
            String encodedTitle = parts[0];
            int score = 0;
            int total = 0;
            long ts = 0L;
            try { score = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
            try { total = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
            try { ts = Long.parseLong(parts[3]); } catch (NumberFormatException ignored) {}
            String title = "";
            try { title = new String(Base64.decode(encodedTitle, Base64.NO_WRAP), StandardCharsets.UTF_8); } catch (Exception ignored) {}

            if (ts > firstTs) {
                secondTs = firstTs; secondTitle = firstTitle; secondScore = firstScore; secondTotal = firstTotal;
                firstTs = ts; firstTitle = title; firstScore = score; firstTotal = total;
            } else if (ts > secondTs) {
                secondTs = ts; secondTitle = title; secondScore = score; secondTotal = total;
            }
        }

        TextView quizTopic1 = root.findViewById(R.id.text_quiz_topic);
        TextView quizProgress1 = root.findViewById(R.id.text_quiz_progress);
        TextView quizTopic2 = root.findViewById(R.id.text_quiz_topic2);
        TextView quizProgress2 = root.findViewById(R.id.text_quiz_progress2);

        // displayed values on 0..100 scale
        int displayed1 = 0;
        int displayed2 = 0;

        if (firstTs < 0) {
            if (quizTopic1 != null) quizTopic1.setText(getString(R.string.placeholder_topic_title));
            if (quizProgress1 != null) quizProgress1.setText("0/100");
        } else {
            if (quizTopic1 != null) quizTopic1.setText(firstTitle == null || firstTitle.isEmpty() ? getString(R.string.placeholder_topic_title) : firstTitle);
            // Treat stored score as a percentage (0..100). Clamp to [0,100].
            displayed1 = Math.max(0, Math.min(firstScore, 100));
            if (quizProgress1 != null) quizProgress1.setText(displayed1 + "/100 (" + displayed1 + "%)");
        }

        if (secondTs < 0) {
            if (quizTopic2 != null) quizTopic2.setText(getString(R.string.placeholder_topic_title));
            if (quizProgress2 != null) quizProgress2.setText("0/100");
        } else {
            if (quizTopic2 != null) quizTopic2.setText(secondTitle == null || secondTitle.isEmpty() ? getString(R.string.placeholder_topic_title) : secondTitle);
            displayed2 = Math.max(0, Math.min(secondScore, 100));
            if (quizProgress2 != null) quizProgress2.setText(displayed2 + "/100 (" + displayed2 + "%)");
        }
        // Note: we intentionally do not update HomeViewModel here; the LiveData-backed UI will be used when available.
    }
}
