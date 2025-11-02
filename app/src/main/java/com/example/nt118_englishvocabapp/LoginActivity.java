package com.example.nt118_englishvocabapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.nt118_englishvocabapp.R;

import com.example.nt118_englishvocabapp.ui.auth.SignInFragment;
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        if (savedInstanceState == null) {
            //Hiển thị Fragment Sign In đầu tiên khi Activity được tạo
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view_auth, new SignInFragment())
                    .commit();
        }
    }

    // Fix lỗi logic sau khi trở về Sign In từ CongaratulationDialogFragment
    public void returnToSignInScreen() {
        // Lấy FragmentManager
        FragmentManager fm = getSupportFragmentManager();
        // Xóa sạch toàn bộ back stack để loại bỏ các màn hình ForgotPassword
        // Tham số thứ 2 là cờ (flag), POP_BACK_STACK_INCLUSIVE sẽ xóa tất cả các trạng thái đã lưu.
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        // Sau khi đã xóa sạch, thay thế container bằng một SignInFragment mới.
        // Vì back stack đã trống, đây sẽ là fragment duy nhất, và nó sẽ hoạt động đúng.
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_view_auth, new SignInFragment())
                .commit();
    }
}
