package com.example.nt118_englishvocabapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.nt118_englishvocabapp.R;

import com.example.nt118_englishvocabapp.ui.auth.SignInFragment;
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (savedInstanceState == null) {
            //Hiển thị Fragment Sign In đầu tiên khi Activity được tạo
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view_auth, new SignInFragment())
                    .commit();
        }
    }
}
