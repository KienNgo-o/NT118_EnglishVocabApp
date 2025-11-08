package com.example.nt118_englishvocabapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.nt118_englishvocabapp.databinding.ActivityMainBinding;
import com.example.nt118_englishvocabapp.ui.flashcard.FlashcardFragment;
import com.example.nt118_englishvocabapp.ui.home.HomeFragment;
import com.example.nt118_englishvocabapp.ui.quiz.QuizFragment;
import com.example.nt118_englishvocabapp.ui.vocab.VocabFragment;
import com.example.nt118_englishvocabapp.ui.translate.TranslateFragment;
import com.example.nt118_englishvocabapp.ui.account.AccountFragment;
import com.example.nt118_englishvocabapp.ui.quiz.ChooseAnswerQuizFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Read saved theme preference and apply it before inflating UI
        try {
            boolean darkEnabled = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .getBoolean("pref_dark_mode", false);
            AppCompatDelegate.setDefaultNightMode(darkEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        } catch (Exception e) {
            // if anything goes wrong, log or ignore and continue with default
            e.printStackTrace();
        }

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        replaceFragment(new HomeFragment());
        binding.bottomNavigationView.setBackground(null);

        binding.bottomNavigationView.setOnItemSelectedListener(selectedMenuItem -> {
            int itemId = selectedMenuItem.getItemId();
            if (itemId == R.id.home) {
                replaceFragment(new HomeFragment());
            } else if (itemId == R.id.flashcard) {
                replaceFragment(new FlashcardFragment());
            } else if (itemId == R.id.quiz) {
                replaceFragment(new QuizFragment());
            } else if (itemId == R.id.vocab) {
                replaceFragment(new VocabFragment());
            }
            return true;
        });

        // Add this block for the FAB
        binding.getRoot().findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                replaceFragment(new ChooseAnswerQuizFragment());
            }
        });
    }


    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    // Public helper so fragments can ensure navigation UI is consistent
    public void navigateToHome() {
        replaceFragment(new HomeFragment());
        // update bottom navigation selection so the highlighted icon matches the shown fragment
        if (binding != null && binding.bottomNavigationView != null) {
            binding.bottomNavigationView.setSelectedItemId(R.id.home);
        }
    }
}