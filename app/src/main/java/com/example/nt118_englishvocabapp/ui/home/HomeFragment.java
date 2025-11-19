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

    private enum DayState { ACTIVE, FREEZE, INACTIVE }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        try {
            binding = FragmentHomeBinding.inflate(inflater, container, false);
            View root = binding.getRoot();

            streakManager = new StreakManager(requireContext());

            // Tìm các view
            TextView quoteText = root.findViewById(R.id.textDialog);
            TextView activeDays = root.findViewById(R.id.text_active_days);
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

            homeViewModel.getActiveDaysText().observe(getViewLifecycleOwner(), s -> {
                if (activeDays != null) activeDays.setText(s);
            });
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
                populateCalendar(calendarGrid, monthLabel, activeDays);
            });
            nextMonth.setOnClickListener(v -> {
                displayMonth++;
                if (displayMonth > 11) {
                    displayMonth = 0;
                    displayYear++;
                }
                populateCalendar(calendarGrid, monthLabel, activeDays);
            });

            // khởi tạo lịch lần đầu
            populateCalendar(calendarGrid, monthLabel, activeDays);

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

    private void populateCalendar(GridLayout grid, TextView monthLabel, TextView activeDaysText) {
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
                    if (resId == R.drawable.streak_no) {
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
                    flame.post(() -> { try { animateToday(flame, cellRoot); } catch (Exception ignored){} });
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

        // cập nhật nhãn số ngày hoạt động
        int totalActive = streakManager.getTotalActiveDays();
        if (activeDaysText != null) {
            activeDaysText.setText(getResources().getQuantityString(R.plurals.active_days_count, totalActive, totalActive));
        }

        // cập nhật số streak trên header
        try {
            TextView headerStreak = null;
            View root = grid.getRootView();
            if (root != null) headerStreak = root.findViewById(R.id.text_streak_number);

            if (headerStreak == null && getActivity() != null) {
                headerStreak = getActivity().findViewById(R.id.text_streak_number);
            }
            if (headerStreak != null) {
                headerStreak.setText(String.valueOf(totalActive));
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
        // Hiệu ứng: icon bắt đầu ở giữa màn hình (kích thước lớn) rồi về vị trí trong ô lịch
        try {
            // Đảm bảo có số đo
            icon.post(() -> {
                try {
                    // giá trị dịch chuyển hiện tại (icon có thể đã có dịch chuyển do layout)
                    float targetTX = icon.getTranslationX();
                    float targetTY = icon.getTranslationY();

                    // tính tâm icon trên màn hình
                    int[] loc = new int[2];
                    icon.getLocationOnScreen(loc);
                    float iconCenterX = loc[0] + icon.getWidth() / 2f;
                    float iconCenterY = loc[1] + icon.getHeight() / 2f;

                    // tâm màn hình
                    final android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                    float screenCenterX = dm.widthPixels / 2f;
                    float screenCenterY = dm.heightPixels / 2f;

                    // tính translation bắt đầu để icon xuất hiện ở tâm màn hình
                    float startTX = screenCenterX - iconCenterX + targetTX;
                    float startTY = screenCenterY - iconCenterY + targetTY;

                    // đặt trạng thái ban đầu: ở giữa màn hình, to và vô hình
                    icon.setTranslationX(startTX);
                    icon.setTranslationY(startTY);
                    icon.setScaleX(3.0f);
                    icon.setScaleY(3.0f);
                    icon.setAlpha(0f);

                    // animate translationX/Y, scale và alpha về giá trị đích
                    ObjectAnimator aTX = ObjectAnimator.ofFloat(icon, "translationX", startTX, targetTX);
                    ObjectAnimator aTY = ObjectAnimator.ofFloat(icon, "translationY", startTY, targetTY);
                    ObjectAnimator sX = ObjectAnimator.ofFloat(icon, "scaleX", 3.0f, 1.0f);
                    ObjectAnimator sY = ObjectAnimator.ofFloat(icon, "scaleY", 3.0f, 1.0f);
                    ObjectAnimator aAlpha = ObjectAnimator.ofFloat(icon, "alpha", 0f, 1f);

                    AnimatorSet arrive = new AnimatorSet();
                    arrive.playTogether(aTX, aTY, sX, sY, aAlpha);
                    arrive.setInterpolator(new OvershootInterpolator(1.6f));
                    arrive.setDuration(700);

                    // nhịp nhỏ trên icon sau khi đến
                    ObjectAnimator pulseX = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.08f);
                    ObjectAnimator pulseY = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.08f);
                    pulseX.setRepeatMode(ValueAnimator.REVERSE);
                    pulseY.setRepeatMode(ValueAnimator.REVERSE);
                    pulseX.setRepeatCount(1);
                    pulseY.setRepeatCount(1);
                    pulseX.setDuration(220);
                    pulseY.setDuration(220);
                    AnimatorSet pulse = new AnimatorSet();
                    pulse.playTogether(pulseX, pulseY);

                    // bounce nhẹ cho pill
                    ObjectAnimator cScaleX = ObjectAnimator.ofFloat(container, "scaleX", 1f, 1.03f);
                    ObjectAnimator cScaleY = ObjectAnimator.ofFloat(container, "scaleY", 1f, 1.03f);
                    cScaleX.setRepeatMode(ValueAnimator.REVERSE);
                    cScaleY.setRepeatMode(ValueAnimator.REVERSE);
                    cScaleX.setRepeatCount(1);
                    cScaleY.setRepeatCount(1);
                    cScaleX.setDuration(220);
                    cScaleY.setDuration(220);
                    AnimatorSet containerPulse = new AnimatorSet();
                    containerPulse.playTogether(cScaleX, cScaleY);

                    AnimatorSet seq = new AnimatorSet();
                    seq.playSequentially(arrive, pulse, containerPulse);
                    seq.start();
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Áp dụng tiến trình flashcard đã lưu làm phương án dự phòng nếu LiveData chưa cập nhật
        try {
            applyLatestProgressFromPrefs(binding != null ? binding.getRoot() : null);
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
}
