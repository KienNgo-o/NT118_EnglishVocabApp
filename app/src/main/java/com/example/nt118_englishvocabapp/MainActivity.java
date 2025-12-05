package com.example.nt118_englishvocabapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.nt118_englishvocabapp.databinding.ActivityMainBinding;
import com.example.nt118_englishvocabapp.ui.flashcard.FlashcardFragment;
import com.example.nt118_englishvocabapp.ui.home.HomeFragment;
import com.example.nt118_englishvocabapp.ui.quiz.QuizListFragment;
import com.example.nt118_englishvocabapp.ui.vocab.VocabFragment;
import com.example.nt118_englishvocabapp.ui.pronounce.PronounceFragment; // NEW: import pronounce fragment

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
            Log.w("MainActivity", "Failed to read theme preference", e);
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
                replaceFragment(new QuizListFragment());
            } else if (itemId == R.id.vocab) {
                replaceFragment(new VocabFragment());
            }
            return true;
        });

        // NEW: navigate to PronounceFragment when FAB (app icon) is clicked
        binding.fab.setOnClickListener(v -> {
            replaceFragment(new PronounceFragment());
            // Optionally, clear bottom nav selection so it doesn't highlight another tab
            try {
                binding.bottomNavigationView.getMenu().setGroupCheckable(0, false, true);
            } catch (Exception ignored) {}
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
        if (binding != null) {
            binding.bottomNavigationView.setSelectedItemId(R.id.home);
        }
    }
}