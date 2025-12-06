package com.example.nt118_englishvocabapp.ui.pronounce;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.util.Log;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.widget.ImageViewCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.media.MediaPlayer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.nt118_englishvocabapp.R;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PronounceFragment extends Fragment {

    private TextView segNormal;
    private TextView segAdvanced;
    // Guard so the dialog is shown only once per fragment instance
    private boolean hasShownDialog = false;
    // Recording state: when true we show the red dot and "Listening" label
    private boolean isRecording = false;

    // UI elements for enhanced recording UI
    private View recordDot;
    private TextView listeningLabel;
    private ImageView micIcon;
    private View micGlow;
    private AnimatorSet glowAnimator;
    private AnimatorSet dotAnimator;
    // Waveform visual and animator
    private com.example.nt118_englishvocabapp.ui.pronounce.WaveformView waveform;
    private ValueAnimator waveformAnimator;
    // Audio capture for real waveform
    private AudioRecord audioRecord;
    private Thread audioThread;
    private volatile boolean audioThreadRunning = false;
    private static final int SAMPLE_RATE = 44100;
    private static final int REQ_RECORD_AUDIO = 1001;
    private boolean pendingStartAfterPermission = false;
    private ActivityResultLauncher<String> permissionLauncher;
    // WAV recording helpers
    private RandomAccessFile wavRaf;
    private long totalAudioBytes = 0;
    private File recordedFile;
    private String currentWordText = null; // captured from WordDetail to name uploaded file
    private MediaPlayer mediaPlayer;
    private AppCompatImageButton btnPlayback;
    private View playbackRow;
    private androidx.core.widget.NestedScrollView scrollView;

    // recording/playback timers
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private long recordStartMs = 0L;
    private boolean isPlayingBack = false;
    // latest audio level written by audio thread and read by UI updater
    private volatile float lastAudioLevel = 0f;
    private Runnable uiLevelRunnable;

    public PronounceFragment() { }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initialize permission launcher to handle RECORD_AUDIO requests
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            boolean grantedPerm = Boolean.TRUE.equals(granted);
            if (grantedPerm && pendingStartAfterPermission) {
                pendingStartAfterPermission = false;
                isRecording = true;
                startRecordingUI();
            } else {
                pendingStartAfterPermission = false;
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pronounce, container, false);

        segNormal = root.findViewById(R.id.seg_normal);
        segAdvanced = root.findViewById(R.id.seg_advanced);

        // initialize visuals to 'normal' selected by default
        setSelected(true);

        View.OnClickListener click = v -> {
            if (v.getId() == R.id.seg_normal) setSelected(true);
            else if (v.getId() == R.id.seg_advanced) setSelected(false);
        };
        segNormal.setOnClickListener(click);
        segAdvanced.setOnClickListener(click);

        // Wire mic button to toggle recording UI (red dot + "Listening")
        View micButton = root.findViewById(R.id.micButton);
        recordDot = root.findViewById(R.id.record_dot);
        // 'listening_label' was removed from layout; use the new record_time TextView
        listeningLabel = root.findViewById(R.id.record_time);
        micIcon = root.findViewById(R.id.mic_icon);
        micGlow = root.findViewById(R.id.mic_glow);
        waveform = root.findViewById(R.id.waveform);

        // assign the glow drawable programmatically (safer across resource merges)
        try {
            if (micGlow != null) micGlow.setBackgroundResource(R.drawable.circle_glow);
        } catch (Throwable t) {
            // fallback: ignore if resource isn't available in some build stages
            Log.w("PronounceFragment", "circle_glow resource not available", t);
        }

        // ensure initial state
        if (recordDot != null) recordDot.setVisibility(View.GONE);
        if (listeningLabel != null) listeningLabel.setVisibility(View.GONE);
        if (micGlow != null) micGlow.setVisibility(View.GONE);
        if (waveform != null) waveform.setVisibility(View.GONE);
        btnPlayback = root.findViewById(R.id.btn_playback);
        playbackRow = root.findViewById(R.id.playback_row);
        scrollView = root.findViewById(R.id.scroll);
        // Apply explicit tint to the remote sound button so the icon is visible across themes
        androidx.appcompat.widget.AppCompatImageButton btnSoundPreview = root.findViewById(R.id.btn_sound);
        if (btnSoundPreview != null) {
            ImageViewCompat.setImageTintList(btnSoundPreview, ContextCompat.getColorStateList(requireContext(), R.color.card_text_color));
            try { btnSoundPreview.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE); } catch (Throwable ignored) {}
        }
        if (btnPlayback != null) {
            btnPlayback.setVisibility(View.GONE);
            // Use app's play icon as the default visual for playback button
            btnPlayback.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_24));
            // Ensure vector uses readable tint color across light/dark themes
            ImageViewCompat.setImageTintList(btnPlayback, ContextCompat.getColorStateList(requireContext(), R.color.card_text_color));
            try { btnPlayback.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE); } catch (Throwable ignored) {}
            btnPlayback.setEnabled(false);
            btnPlayback.setAlpha(0.6f);
            btnPlayback.setContentDescription(getString(R.string.cd_playback));
        }
        if (playbackRow != null) playbackRow.setVisibility(View.GONE);
        if (micIcon != null) { micIcon.setScaleX(0.8f); micIcon.setScaleY(0.8f); }

        // Header return button: go back
        View btnReturn = root.findViewById(R.id.btn_return);
        if (btnReturn != null) btnReturn.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        // --- New: load word details when this fragment receives a word_id argument ---
        final com.example.nt118_englishvocabapp.network.ApiService apiService = com.example.nt118_englishvocabapp.network.RetrofitClient.getApiService(requireContext());
        Bundle args = getArguments();
        if (args != null && args.containsKey("word_id")) {
            int wordId = args.getInt("word_id", -1);
            if (wordId >= 0) {
                apiService.getWordDetails(wordId).enqueue(new retrofit2.Callback<com.example.nt118_englishvocabapp.models.WordDetail>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.nt118_englishvocabapp.models.WordDetail> call, retrofit2.Response<com.example.nt118_englishvocabapp.models.WordDetail> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.example.nt118_englishvocabapp.models.WordDetail wd = response.body();

                            android.widget.TextView tvWord = root.findViewById(R.id.word);
                            android.widget.TextView tvPhon = root.findViewById(R.id.phonetic);
                            android.widget.TextView tvDef = root.findViewById(R.id.definition);
                            androidx.appcompat.widget.AppCompatImageButton btnSound = root.findViewById(R.id.btn_sound);

                            if (tvWord != null) tvWord.setText(wd.getWordText() != null ? wd.getWordText() : "");
                            // remember word text for naming uploads
                            try { currentWordText = wd.getWordText(); } catch (Throwable ignored) {}

                            String phon = null;
                            try {
                                if (wd.getPronunciations() != null && !wd.getPronunciations().isEmpty())
                                    phon = wd.getPronunciations().get(0).getPhoneticSpelling();
                            } catch (Exception ignored) {}
                            if (tvPhon != null) tvPhon.setText(phon != null ? phon : "");

                            String def = null;
                            try {
                                if (wd.getDefinitions() != null && !wd.getDefinitions().isEmpty())
                                    def = wd.getDefinitions().get(0).getDefinitionText();
                            } catch (Exception ignored) {}
                            if (tvDef != null) tvDef.setText(def != null ? def : "");

                            // Wire sound button to first pronunciation audio if available
                            String audioUrl = null;
                            try {
                                if (wd.getPronunciations() != null && !wd.getPronunciations().isEmpty()) {
                                    for (com.example.nt118_englishvocabapp.models.Pronunciation p : wd.getPronunciations()) {
                                        if (p == null) continue;
                                        String u = p.getAudioFileUrl();
                                        if (u == null) continue;
                                        u = u.trim();
                                        // remove accidental CR/LF characters that may be in some backends
                                        u = u.replaceAll("\\r|\\n", "");
                                        if (!u.isEmpty()) { audioUrl = u; break; }
                                    }
                                }
                            } catch (Exception ignored) {}

                            if (btnSound != null) {
                                if (audioUrl != null && !audioUrl.isEmpty()) {
                                    btnSound.setVisibility(View.VISIBLE);
                                    final String finalAudio = audioUrl;
                                    btnSound.setOnClickListener(v -> {
                                        try {
                                            // stop/release previous player if any
                                            if (mediaPlayer != null) {
                                                try { mediaPlayer.stop(); } catch (Exception ignored) {}
                                                try { mediaPlayer.release(); } catch (Exception ignored) {}
                                                mediaPlayer = null;
                                            }
                                            mediaPlayer = new MediaPlayer();
                                            mediaPlayer.setDataSource(finalAudio);
                                            mediaPlayer.setOnPreparedListener(mp -> mp.start());
                                            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                                                // hide button on unrecoverable playback error
                                                try { btnSound.setVisibility(View.GONE); } catch (Exception ignored) {}
                                                return true;
                                            });
                                            mediaPlayer.prepareAsync();
                                        } catch (IOException e) {
                                            Log.w("PronounceFragment", "failed to play audio: " + finalAudio, e);
                                            try { btnSound.setVisibility(View.GONE); } catch (Exception ignored) {}
                                        }
                                    });
                                } else {
                                    btnSound.setVisibility(View.GONE);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.example.nt118_englishvocabapp.models.WordDetail> call, Throwable t) {
                        // silent failure - keep default UI
                    }
                });
            }
        }

        if (micButton != null) {
            micButton.setOnClickListener(v -> {
                if (!isRecording) {
                    // user wants to start recording: ensure we have permission
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        isRecording = true;
                        startRecordingUI();
                    } else {
                        // request permission via Activity Result API and remember intent
                        pendingStartAfterPermission = true;
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
                    }
                } else {
                    // stop
                    isRecording = false;
                    stopRecordingUI();
                }
            });
        }

        // playback click: toggle play/pause of last recorded file
        if (btnPlayback != null) {
            btnPlayback.setOnClickListener(v -> {
                if (recordedFile == null || !recordedFile.exists()) return;
                if (isPlayingBack) {
                    // stop playback
                    stopPlayback();
                    return;
                }
                startPlayback();
            });
        }

        // Show the pronounce dialog after the view is laid out (i.e. after navigation to this fragment)
        root.post(() -> {
            if (!hasShownDialog && isAdded()) {
                // Use the DialogFragment which handles lifecycle and configuration changes.
                PronounceDialogFragment dlg = new PronounceDialogFragment();
                dlg.show(getParentFragmentManager(), "pronounce_dialog");
                hasShownDialog = true;
            }
        });

        return root;
    }

    private void startRecordingUI() {
        Log.d("PronounceFragment", "startRecordingUI() called");
        // show listening label + red dot
        if (recordDot != null) recordDot.setVisibility(View.VISIBLE);
        if (listeningLabel != null) listeningLabel.setVisibility(View.VISIBLE);
        // hide playback while recording a new clip
        // Ensure the playback row (which contains waveform and time) is visible during recording
        // so the waveform and time counter can be shown. Only hide/disable the playback button.
        if (playbackRow != null) {
             playbackRow.setVisibility(View.VISIBLE);
             playbackRow.invalidate(); playbackRow.requestLayout(); playbackRow.bringToFront();
             Log.d("PronounceFragment", "playbackRow set VISIBLE");
         }
         if (btnPlayback != null) {
             btnPlayback.setVisibility(View.GONE);
             // Use app's play icon as the default visual for playback button
             btnPlayback.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_24));
             // Ensure vector uses readable tint color across light/dark themes
             ImageViewCompat.setImageTintList(btnPlayback, ContextCompat.getColorStateList(requireContext(), R.color.card_text_color));
             try { btnPlayback.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE); } catch (Throwable ignored) {}
             btnPlayback.setEnabled(false);
             btnPlayback.setAlpha(0.6f);
             btnPlayback.setContentDescription(getString(R.string.cd_playback));
             btnPlayback.invalidate(); btnPlayback.requestLayout();
             Log.d("PronounceFragment", "btnPlayback hidden/disabled");
         }
        if (listeningLabel != null) {
            listeningLabel.setText(formatTimeMs(0));
            // increase visibility: darker and slightly larger
            try {
                listeningLabel.setTextColor(0xFF222222);
                listeningLabel.setTextSize(16f);
            } catch (Throwable ignored) {}
        }
        // show waveform and start real audio capture; if permission missing, fallback to simulated
        if (waveform != null) {
            waveform.setVisibility(View.VISIBLE);
            waveform.invalidate(); waveform.requestLayout(); waveform.bringToFront();
            Log.d("PronounceFragment", "waveform set VISIBLE");
        }
        if (listeningLabel != null) {
            listeningLabel.setVisibility(View.VISIBLE);
            listeningLabel.invalidate(); listeningLabel.requestLayout(); listeningLabel.bringToFront();
        }
        // Ensure the playback row is visible on screen (scroll to it)
        if (scrollView != null && playbackRow != null) {
            scrollView.post(() -> {
                try {
                    scrollView.smoothScrollTo(0, playbackRow.getTop());
                } catch (Throwable t) {
                    Log.w("PronounceFragment", "scroll to playbackRow failed", t);
                }
            });
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startAudioCapture();
        } else {
            // fallback simulated waveform if permission not yet granted
            if (waveform != null) {
                waveformAnimator = ValueAnimator.ofFloat(0f, 1f);
                waveformAnimator.setDuration(120);
                waveformAnimator.setRepeatCount(ValueAnimator.INFINITE);
                waveformAnimator.addUpdateListener(animation -> {
                    float level = 0.05f + (float)Math.random() * 0.9f;
                    waveform.setLevel(level);
                });
                waveformAnimator.start();
            }
        }
        if (listeningLabel != null) { listeningLabel.invalidate(); listeningLabel.requestLayout(); Log.d("PronounceFragment", "listeningLabel visible and reset to 0:00"); }

        // reset previous recorded file while starting a new recording
        if (recordedFile != null && recordedFile.exists()) {
            // keep previous file on disk but hide playback until new recording finished
        }

        // animate mic icon to slightly larger (feedback)
        if (micIcon != null) {
            micIcon.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
            micIcon.setContentDescription(getString(R.string.listening_label_text));
        }

        // start timer to update record_time label
        recordStartMs = System.currentTimeMillis();
        if (listeningLabel != null) {
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    long elapsed = System.currentTimeMillis() - recordStartMs;
                    listeningLabel.setText(formatTimeMs(elapsed));
                    timerHandler.postDelayed(this, 200);
                }
            };
            timerHandler.post(timerRunnable);
        }
        // start UI updater that applies the latest audio level to the waveform regularly
        uiLevelRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    float lvl = lastAudioLevel;
                    if (waveform != null) waveform.setLevel(lvl);
                try { if (waveform != null) waveform.postInvalidate(); } catch (Throwable ignored) {}
                } catch (Throwable ignored) {}
                timerHandler.postDelayed(this, 100);
            }
        };
        timerHandler.post(uiLevelRunnable);

        // prepare and start glow animation behind mic
        if (micGlow != null) {
            micGlow.setVisibility(View.VISIBLE);
            micGlow.setScaleX(0.9f);
            micGlow.setScaleY(0.9f);
            micGlow.setAlpha(0.9f);

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(micGlow, "scaleX", 0.9f, 1.18f);
            scaleX.setDuration(800);
            scaleX.setRepeatCount(ValueAnimator.INFINITE);
            scaleX.setRepeatMode(ValueAnimator.REVERSE);

            ObjectAnimator scaleY = ObjectAnimator.ofFloat(micGlow, "scaleY", 0.9f, 1.18f);
            scaleY.setDuration(800);
            scaleY.setRepeatCount(ValueAnimator.INFINITE);
            scaleY.setRepeatMode(ValueAnimator.REVERSE);

            ObjectAnimator alpha = ObjectAnimator.ofFloat(micGlow, "alpha", 0.55f, 0.18f);
            alpha.setDuration(800);
            alpha.setRepeatCount(ValueAnimator.INFINITE);
            alpha.setRepeatMode(ValueAnimator.REVERSE);

            glowAnimator = new AnimatorSet();
            glowAnimator.playTogether(scaleX, scaleY, alpha);
            glowAnimator.start();
        }

        // animate red dot pulsing
        if (recordDot != null) {
            recordDot.setScaleX(1f);
            recordDot.setScaleY(1f);
            ObjectAnimator dotScaleX = ObjectAnimator.ofFloat(recordDot, "scaleX", 1f, 1.6f);
            dotScaleX.setDuration(650);
            dotScaleX.setRepeatCount(ValueAnimator.INFINITE);
            dotScaleX.setRepeatMode(ValueAnimator.REVERSE);

            ObjectAnimator dotScaleY = ObjectAnimator.ofFloat(recordDot, "scaleY", 1f, 1.6f);
            dotScaleY.setDuration(650);
            dotScaleY.setRepeatCount(ValueAnimator.INFINITE);
            dotScaleY.setRepeatMode(ValueAnimator.REVERSE);

            ObjectAnimator dotAlpha = ObjectAnimator.ofFloat(recordDot, "alpha", 1f, 0.45f);
            dotAlpha.setDuration(650);
            dotAlpha.setRepeatCount(ValueAnimator.INFINITE);
            dotAlpha.setRepeatMode(ValueAnimator.REVERSE);

            dotAnimator = new AnimatorSet();
            dotAnimator.playTogether(dotScaleX, dotScaleY, dotAlpha);
            dotAnimator.start();
        }
    }

    private void stopRecordingUI() {
        // hide listening label + red dot
        if (listeningLabel != null) {
            // stop timer and show final duration
            if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
            if (recordStartMs > 0) listeningLabel.setText(formatTimeMs(System.currentTimeMillis() - recordStartMs));
            listeningLabel.setVisibility(View.VISIBLE);
        }
        // stop waveform and audio capture
        stopAudioCapture();
        if (waveformAnimator != null) {
            waveformAnimator.cancel();
            waveformAnimator = null;
        }
        if (waveform != null) waveform.setVisibility(View.GONE);

        // animate mic icon back to resting size
        if (micIcon != null) {
            micIcon.animate().scaleX(0.8f).scaleY(0.8f).setDuration(180).start();
            micIcon.setContentDescription(getString(R.string.cd_microphone_icon));
        }

        // stop glow animation and hide
        if (glowAnimator != null) {
            glowAnimator.cancel();
            glowAnimator = null;
        }
        if (micGlow != null) micGlow.setVisibility(View.GONE);

        // stop dot animation and hide
        if (dotAnimator != null) {
            dotAnimator.cancel();
            dotAnimator = null;
        }
        if (recordDot != null) recordDot.setVisibility(View.GONE);
        // If a recorded file exists, reveal playback button and keep the duration visible
        if (recordedFile != null && recordedFile.exists() && btnPlayback != null) {
            btnPlayback.setVisibility(View.VISIBLE);
        }
        // Force the playback row and button visible; enable the button only if we have a recorded file
        if (playbackRow != null) playbackRow.setVisibility(View.VISIBLE);
        if (btnPlayback != null) {
            btnPlayback.setVisibility(View.VISIBLE);
            boolean hasFile = recordedFile != null && recordedFile.exists();
            btnPlayback.setEnabled(hasFile);
            btnPlayback.setAlpha(hasFile ? 1f : 0.6f);
            btnPlayback.invalidate();
            btnPlayback.requestLayout();
            btnPlayback.bringToFront();
        }
        // stop the UI-level updater
        if (uiLevelRunnable != null) {
            timerHandler.removeCallbacks(uiLevelRunnable);
            uiLevelRunnable = null;
        }

        // After finalizing the WAV file, upload it to server for grading if we have a valid word_id argument
        try {
            Bundle args = getArguments();
            if (args != null && args.containsKey("word_id") && recordedFile != null && recordedFile.exists()) {
                int wordId = args.getInt("word_id", -1);
                if (wordId >= 0) {
                    uploadRecordedAudio(wordId, recordedFile);
                }
            }
        } catch (Throwable ignored) {}
    }

    // Upload recordedFile to /api/pronun/grade as multipart form-data:
    // - word_id (text)
    // - user_audio (file) -> filename kept as recordedFile.getName()
    private void uploadRecordedAudio(int wordId, File recordedFile) {
        try {
            com.example.nt118_englishvocabapp.network.ApiService api = com.example.nt118_englishvocabapp.network.RetrofitClient.getApiService(requireContext());

            // Create RequestBody for word_id (as text)
            RequestBody wordIdBody = RequestBody.create(String.valueOf(wordId), MediaType.parse("text/plain"));

            // Create RequestBody and MultipartBody.Part for file
            String mime = "audio/wav";
            RequestBody fileBody = RequestBody.create(recordedFile, MediaType.parse(mime));
            // Prefer a friendly filename derived from word text when available
            String uploadFileName = recordedFile.getName();
            try {
                if (currentWordText != null && !currentWordText.trim().isEmpty()) {
                    // sanitize to simple filename: keep letters/numbers/underscore
                    String safe = currentWordText.trim().replaceAll("[^A-Za-z0-9_\\-]", "_");
                    uploadFileName = safe + ".mp3"; // filename requested by backend examples
                }
            } catch (Throwable ignored) {}
            MultipartBody.Part part = MultipartBody.Part.createFormData("user_audio", uploadFileName, fileBody);

            // Show an indeterminate progress dialog while uploading + waiting for grading
            final String progressTag = "pronun_progress";
            PronunProgressDialogFragment progress = PronunProgressDialogFragment.newInstance("Grading your pronunciation...");
            try { progress.show(getParentFragmentManager(), progressTag); } catch (Throwable ignored) {}

            Call<com.example.nt118_englishvocabapp.models.PronunGradeResponse> call = api.postPronunGrade(wordIdBody, part);
            call.enqueue(new Callback<com.example.nt118_englishvocabapp.models.PronunGradeResponse>() {
                 @Override
                 public void onResponse(Call<com.example.nt118_englishvocabapp.models.PronunGradeResponse> call, Response<com.example.nt118_englishvocabapp.models.PronunGradeResponse> response) {
                    // dismiss progress dialog if present
                    try {
                        androidx.fragment.app.Fragment f = getParentFragmentManager().findFragmentByTag(progressTag);
                        if (f != null && f instanceof PronunProgressDialogFragment) {
                            ((PronunProgressDialogFragment) f).dismissAllowingStateLoss();
                        }
                    } catch (Throwable ignored) {}

                    if (response.isSuccessful() && response.body() != null) {
                         android.util.Log.d("PronounceFragment", "uploadRecordedAudio: success for wordId=" + wordId);
                         final com.example.nt118_englishvocabapp.models.PronunGradeResponse body = response.body();
                         // Some backends need a little time to finish grading; delay 500ms before showing dialog so server can finalize
                         timerHandler.postDelayed(() -> {
                             if (!isAdded()) return;
                             double score = 0.0;
                             String feedback = "";
                             try {
                                 if (body.getData() != null) {
                                     score = body.getData().getScore();
                                     feedback = body.getData().getFeedback();
                                 }
                             } catch (Throwable ignored) {}

                             // Clamp score to 100 for display if backend returns >100
                             try { score = Math.min(100.0, score); } catch (Throwable ignored) {}

                             // Fallback feedback rules when server returned empty feedback
                             if (feedback == null || feedback.trim().isEmpty()) {
                                 if (score > 80.0) {
                                     feedback = "Great job, you sounds like a native speaker";
                                 } else if (score > 50.0 && score < 80.0) {
                                     feedback = "Good job, AI found no mistake in you pronunciation";
                                 } else {
                                     feedback = "(no feedback)";
                                 }
                             }

                             PronunGradeResultDialogFragment dlg = PronunGradeResultDialogFragment.newInstance(score, feedback);
                             try { dlg.show(getParentFragmentManager(), "pronun_grade_result"); } catch (Throwable t) { android.util.Log.w("PronounceFragment","show dialog failed", t); }
                         }, 500);
                     } else {
                         android.util.Log.w("PronounceFragment", "uploadRecordedAudio: server returned " + (response == null ? "<null>" : response.code()));
                     }
                 }

                 @Override
                 public void onFailure(Call<com.example.nt118_englishvocabapp.models.PronunGradeResponse> call, Throwable t) {
                    // dismiss progress dialog if present
                    try {
                        androidx.fragment.app.Fragment f = getParentFragmentManager().findFragmentByTag(progressTag);
                        if (f != null && f instanceof PronunProgressDialogFragment) {
                            ((PronunProgressDialogFragment) f).dismissAllowingStateLoss();
                        }
                    } catch (Throwable ignored) {}

                    android.util.Log.w("PronounceFragment", "uploadRecordedAudio: failed", t);
                 }
             });
        } catch (Exception e) {
            android.util.Log.w("PronounceFragment", "uploadRecordedAudio threw", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // ensure animators are stopped to avoid leaks
        if (glowAnimator != null) {
            glowAnimator.cancel();
            glowAnimator = null;
        }
        if (dotAnimator != null) {
            dotAnimator.cancel();
            dotAnimator = null;
        }
        if (waveformAnimator != null) {
            waveformAnimator.cancel();
            waveformAnimator = null;
        }
        stopAudioCapture();
    }

    // Audio capture helpers
    private void startAudioCapture() {
        // Ensure we have RECORD_AUDIO permission before creating AudioRecord
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w("PronounceFragment", "startAudioCapture called without RECORD_AUDIO permission");
            return;
        }
        if (audioThreadRunning) return;
        try {
            int minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            int bufferSize = Math.max(minBuf, SAMPLE_RATE / 10);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.w("PronounceFragment", "AudioRecord not initialized");
                return;
            }
            // prepare WAV file to write captured PCM
            try {
                recordedFile = new File(requireContext().getCacheDir(), "pronounce_last.wav");
                wavRaf = new RandomAccessFile(recordedFile, "rw");
                wavRaf.setLength(0);
                // reserve 44 bytes for WAV header
                wavRaf.write(new byte[44]);
                totalAudioBytes = 0;
            } catch (IOException e) {
                Log.w("PronounceFragment", "cannot create wav file", e);
                recordedFile = null;
                wavRaf = null;
            }

            audioRecord.startRecording();
            audioThreadRunning = true;
            audioThread = new Thread(() -> {
                short[] buffer = new short[bufferSize / 2];
                Handler main = new Handler(Looper.getMainLooper());
                byte[] byteBuf = new byte[buffer.length * 2];
                while (audioThreadRunning && audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        // compute RMS for waveform
                        double sum = 0;
                        for (int i = 0; i < read; i++) {
                            double v = buffer[i] / 32768.0;
                            sum += v * v;
                        }
                        double rms = read > 0 ? Math.sqrt(sum / read) : 0.0;
                        float level = (float) Math.min(1.0, rms * 2.5); // scale a bit to be visible
                        // publish level for UI updater to consume on main thread
                        lastAudioLevel = level;
                        if (waveform != null) {
                            final float lev = level;
                            main.post(() -> {
                                // immediate update as well (keeps UI snappy)
                                waveform.setLevel(lev);
                                // ensure view redraw
                                try { waveform.postInvalidate(); } catch (Throwable ignored) {}
                                if (System.currentTimeMillis() % 1000 < 50) Log.d("PronounceFragment", "waveform level=" + lev);
                            });
                        }

                        // write PCM little-endian to wavRaf if present
                        if (wavRaf != null) {
                            int bytePos = 0;
                            for (int i = 0; i < read; i++) {
                                short s = buffer[i];
                                byteBuf[bytePos++] = (byte) (s & 0x00ff);
                                byteBuf[bytePos++] = (byte) ((s >> 8) & 0x00ff);
                            }
                            try {
                                wavRaf.write(byteBuf, 0, bytePos);
                                totalAudioBytes += bytePos;
                            } catch (IOException e) {
                                Log.w("PronounceFragment", "write wav failed", e);
                            }
                        }
                    }
                    try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                }
            }, "AudioCapture");
            audioThread.start();
        } catch (Throwable t) {
            Log.w("PronounceFragment", "startAudioCapture failed", t);
        }
    }

    private void stopAudioCapture() {
        audioThreadRunning = false;
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) audioRecord.stop();
            } catch (Throwable ignored) {}
            audioRecord.release();
            audioRecord = null;
        }
        if (audioThread != null) {
            try { audioThread.join(300); } catch (InterruptedException ignored) {}
            audioThread = null;
        }
        // finalize WAV header if we wrote data
        if (wavRaf != null) {
            try {
                long totalDataLen = totalAudioBytes + 36;
                int channels = 1;
                long byteRate = SAMPLE_RATE * 2 * channels;
                writeWavHeader(wavRaf, totalAudioBytes, totalDataLen, SAMPLE_RATE, channels, byteRate);
                wavRaf.close();
                wavRaf = null;
                // Reveal and enable playback button if there is a recorded file
                if (btnPlayback != null) {
                    boolean hasFile = recordedFile != null && recordedFile.exists();
                    btnPlayback.setVisibility(View.VISIBLE);
                    // show play icon when enabling playback
                    btnPlayback.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_24));
                    ImageViewCompat.setImageTintList(btnPlayback, ContextCompat.getColorStateList(requireContext(), R.color.card_text_color));
                    btnPlayback.setEnabled(hasFile);
                    btnPlayback.setAlpha(hasFile ? 1f : 0.6f);
                }
                if (playbackRow != null) playbackRow.setVisibility(View.VISIBLE);
             } catch (IOException e) {
                 Log.w("PronounceFragment", "finalize wav failed", e);
             }
         }
     }

    private void writeWavHeader(RandomAccessFile raf, long totalAudioLen, long totalDataLen, int sampleRate, int channels, long byteRate) throws IOException {
        raf.seek(0);
        raf.writeBytes("RIFF");
        raf.writeInt(Integer.reverseBytes((int) totalDataLen));
        raf.writeBytes("WAVE");
        raf.writeBytes("fmt ");
        raf.writeInt(Integer.reverseBytes(16)); // Subchunk1Size for PCM
        raf.writeShort(Short.reverseBytes((short)1)); // AudioFormat = 1
        raf.writeShort(Short.reverseBytes((short)channels));
        raf.writeInt(Integer.reverseBytes(sampleRate));
        raf.writeInt(Integer.reverseBytes((int)byteRate));
        raf.writeShort(Short.reverseBytes((short)(channels * 2))); // block align
        raf.writeShort(Short.reverseBytes((short)16)); // bits per sample
        raf.writeBytes("data");
        raf.writeInt(Integer.reverseBytes((int) totalAudioLen));
    }

    @Override
    public void onStop() {
        super.onStop();
        // stop playback if active
        if (mediaPlayer != null) {
            try { if (mediaPlayer.isPlaying()) mediaPlayer.stop(); } catch (Throwable ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void setSelected(boolean normalSelected) {
        // Use view's selected state so the tab_selector drawable and tab_text_color selector
        // provide the active pill visuals exactly the same as the vocab fragments.
        segNormal.setSelected(normalSelected);
        segAdvanced.setSelected(!normalSelected);
    }

    private void startPlayback() {
        if (recordedFile == null || !recordedFile.exists()) return;
        isPlayingBack = true;
        if (listeningLabel != null) listeningLabel.setVisibility(View.VISIBLE);
        if (btnPlayback != null) {
            btnPlayback.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_pause_24));
            ImageViewCompat.setImageTintList(btnPlayback, ContextCompat.getColorStateList(requireContext(), R.color.card_text_color));
            btnPlayback.setContentDescription(getString(R.string.cd_pause));
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(recordedFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            // update timer during playback
            recordStartMs = System.currentTimeMillis();
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        listeningLabel.setText(formatTimeMs(mediaPlayer.getCurrentPosition()));
                        timerHandler.postDelayed(this, 200);
                    }
                }
            };
            timerHandler.post(timerRunnable);
            // show pause icon while playing
            if (btnPlayback != null) {
                btnPlayback.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_pause_24));
                ImageViewCompat.setImageTintList(btnPlayback, ContextCompat.getColorStateList(requireContext(), R.color.card_text_color));
                btnPlayback.setContentDescription(getString(R.string.cd_pause));
            }
            mediaPlayer.setOnCompletionListener(mp -> stopPlayback());
        } catch (IOException e) {
            Log.w("PronounceFragment", "playback failed", e);
            stopPlayback();
        }
    }

    private void stopPlayback() {
        isPlayingBack = false;
        if (btnPlayback != null) {
            btnPlayback.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_play_24));
            ImageViewCompat.setImageTintList(btnPlayback, ContextCompat.getColorStateList(requireContext(), R.color.card_text_color));
            btnPlayback.setContentDescription(getString(R.string.cd_playback));
        }
        if (mediaPlayer != null) {
            try { if (mediaPlayer.isPlaying()) mediaPlayer.stop(); } catch (Throwable ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
    }

    private String formatTimeMs(long ms) {
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format(java.util.Locale.getDefault(), "%d:%02d", min, sec);
    }
}
