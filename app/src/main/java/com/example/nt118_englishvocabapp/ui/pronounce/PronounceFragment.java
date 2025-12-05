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

import com.example.nt118_englishvocabapp.R;

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
    // WAV recording helpers
    private RandomAccessFile wavRaf;
    private long totalAudioBytes = 0;
    private File recordedFile;
    private MediaPlayer mediaPlayer;
    private View btnPlayback;

    public PronounceFragment() { }

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
        listeningLabel = root.findViewById(R.id.listening_label);
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
        if (btnPlayback != null) btnPlayback.setVisibility(View.GONE);
        if (micIcon != null) { micIcon.setScaleX(0.8f); micIcon.setScaleY(0.8f); }

        // Header return button: go back
        View btnReturn = root.findViewById(R.id.btn_return);
        if (btnReturn != null) btnReturn.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        if (micButton != null) {
            micButton.setOnClickListener(v -> {
                if (!isRecording) {
                    // user wants to start recording: ensure we have permission
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        isRecording = true;
                        startRecordingUI();
                    } else {
                        // request permission and remember intent
                        pendingStartAfterPermission = true;
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
                    }
                } else {
                    // stop
                    isRecording = false;
                    stopRecordingUI();
                }
            });
        }

        // playback click: play last recorded file if available
        if (btnPlayback != null) {
            btnPlayback.setOnClickListener(v -> {
                if (recordedFile == null || !recordedFile.exists()) return;
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    // update UI icon if needed
                    return;
                }
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(recordedFile.getAbsolutePath());
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    mediaPlayer.setOnCompletionListener(mp -> {
                        mp.release();
                        mediaPlayer = null;
                    });
                } catch (IOException e) {
                    Log.w("PronounceFragment", "playback failed", e);
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }
                }
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
        // show listening label + red dot
        if (recordDot != null) recordDot.setVisibility(View.VISIBLE);
        if (listeningLabel != null) listeningLabel.setVisibility(View.VISIBLE);
        // show waveform and start real audio capture; if permission missing, fallback to simulated
        if (waveform != null) waveform.setVisibility(View.VISIBLE);
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

        // animate mic icon to slightly larger (feedback)
        if (micIcon != null) {
            micIcon.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
            micIcon.setContentDescription(getString(R.string.listening_label_text));
        }

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
        if (listeningLabel != null) listeningLabel.setVisibility(View.GONE);
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
                        if (waveform != null) main.post(() -> waveform.setLevel(level));

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
                // Reveal playback button if there is a recorded file
                if (recordedFile != null && recordedFile.exists() && btnPlayback != null) btnPlayback.setVisibility(View.VISIBLE);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted && pendingStartAfterPermission) {
                // start recording now
                pendingStartAfterPermission = false;
                isRecording = true;
                startRecordingUI();
            } else {
                pendingStartAfterPermission = false;
            }
        }
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
}
