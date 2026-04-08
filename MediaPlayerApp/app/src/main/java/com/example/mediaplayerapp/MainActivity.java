package com.example.mediaplayerapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // ─── VIDEO ────────────────────────────────────────────────────────────────
    private VideoView videoView;
    private View videoPlaceholder;
    private EditText etVideoUrl;
    private Button btnOpenUrl, btnVideoPlay, btnVideoPause, btnVideoStop, btnVideoRestart;
    private String currentVideoUrl = null;
    private MediaController mediaController;

    // ─── AUDIO ────────────────────────────────────────────────────────────────
    private MediaPlayer mediaPlayer;
    private Button btnOpenFile, btnAudioPlay, btnAudioPause, btnAudioStop, btnAudioRestart;
    private TextView tvAudioTitle, tvAudioStatus, tvCurrentTime, tvTotalTime;
    private SeekBar seekBarAudio;
    private Uri audioUri = null;
    private boolean isAudioPrepared = false;

    // SeekBar update handler
    private final Handler seekHandler = new Handler(Looper.getMainLooper());
    private final Runnable seekUpdater = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isAudioPrepared && mediaPlayer.isPlaying()) {
                int current = mediaPlayer.getCurrentPosition();
                int total   = mediaPlayer.getDuration();
                seekBarAudio.setMax(total);
                seekBarAudio.setProgress(current);
                tvCurrentTime.setText(formatTime(current));
                tvTotalTime.setText(formatTime(total));
            }
            seekHandler.postDelayed(this, 500);
        }
    };

    // File picker launcher
    private final ActivityResultLauncher<String> audioPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    audioUri = uri;
                    String fileName = uri.getLastPathSegment();
                    tvAudioTitle.setText(fileName != null ? fileName : uri.toString());
                    tvAudioStatus.setText("Status: File loaded. Press Play.");
                    resetAudioPlayer();
                }
            });

    // Runtime permission launcher
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean granted = result.containsValue(Boolean.TRUE);
                if (granted) {
                    audioPickerLauncher.launch("audio/*");
                } else {
                    Toast.makeText(this, "Storage permission denied.", Toast.LENGTH_SHORT).show();
                }
            });

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initVideoViews();
        initAudioViews();
        setVideoListeners();
        setAudioListeners();

        // Start seek-bar updater loop
        seekHandler.post(seekUpdater);
    }

    // =========================================================================
    //  INIT
    // =========================================================================

    private void initVideoViews() {
        videoView        = findViewById(R.id.videoView);
        videoPlaceholder = findViewById(R.id.videoPlaceholder);
        etVideoUrl       = findViewById(R.id.etVideoUrl);
        btnOpenUrl       = findViewById(R.id.btnOpenUrl);
        btnVideoPlay     = findViewById(R.id.btnVideoPlay);
        btnVideoPause    = findViewById(R.id.btnVideoPause);
        btnVideoStop     = findViewById(R.id.btnVideoStop);
        btnVideoRestart  = findViewById(R.id.btnVideoRestart);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
    }

    private void initAudioViews() {
        btnOpenFile      = findViewById(R.id.btnOpenFile);
        btnAudioPlay     = findViewById(R.id.btnAudioPlay);
        btnAudioPause    = findViewById(R.id.btnAudioPause);
        btnAudioStop     = findViewById(R.id.btnAudioStop);
        btnAudioRestart  = findViewById(R.id.btnAudioRestart);
        tvAudioTitle     = findViewById(R.id.tvAudioTitle);
        tvAudioStatus    = findViewById(R.id.tvAudioStatus);
        tvCurrentTime    = findViewById(R.id.tvCurrentTime);
        tvTotalTime      = findViewById(R.id.tvTotalTime);
        seekBarAudio     = findViewById(R.id.seekBarAudio);
    }

    // =========================================================================
    //  VIDEO LISTENERS
    // =========================================================================

    private void setVideoListeners() {

        // Open URL
        btnOpenUrl.setOnClickListener(v -> {
            String url = etVideoUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
                return;
            }
            currentVideoUrl = url;
            loadVideo(Uri.parse(url));
        });

        // Play
        btnVideoPlay.setOnClickListener(v -> {
            if (currentVideoUrl == null) {
                Toast.makeText(this, "Open a URL first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!videoView.isPlaying()) {
                videoView.start();
                videoPlaceholder.setVisibility(View.GONE);
                Toast.makeText(this, "▶ Playing video", Toast.LENGTH_SHORT).show();
            }
        });

        // Pause
        btnVideoPause.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                Toast.makeText(this, "⏸ Video paused", Toast.LENGTH_SHORT).show();
            }
        });

        // Stop
        btnVideoStop.setOnClickListener(v -> {
            if (currentVideoUrl != null) {
                videoView.stopPlayback();
                videoPlaceholder.setVisibility(View.VISIBLE);
                currentVideoUrl = null;
                Toast.makeText(this, "⏹ Video stopped", Toast.LENGTH_SHORT).show();
            }
        });

        // Restart
        btnVideoRestart.setOnClickListener(v -> {
            if (currentVideoUrl != null) {
                loadVideo(Uri.parse(currentVideoUrl));
                Toast.makeText(this, "↺ Video restarted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Open a URL first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadVideo(Uri uri) {
        videoPlaceholder.setVisibility(View.GONE);
        videoView.setVideoURI(uri);
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            videoView.start();
        });
        videoView.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(this, "Error loading video. Check URL.", Toast.LENGTH_LONG).show();
            videoPlaceholder.setVisibility(View.VISIBLE);
            return true;
        });
        videoView.setOnCompletionListener(mp ->
                Toast.makeText(this, "✅ Video playback complete", Toast.LENGTH_SHORT).show()
        );
    }

    // =========================================================================
    //  AUDIO LISTENERS
    // =========================================================================

    private void setAudioListeners() {

        // Open File
        btnOpenFile.setOnClickListener(v -> checkPermissionAndPickFile());

        // Play
        btnAudioPlay.setOnClickListener(v -> {
            if (audioUri == null) {
                Toast.makeText(this, "Open an audio file first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isAudioPrepared) {
                prepareAndPlay();
            } else if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                tvAudioStatus.setText("Status: ▶ Playing");
            }
        });

        // Pause
        btnAudioPause.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                tvAudioStatus.setText("Status: ⏸ Paused");
            }
        });

        // Stop
        btnAudioStop.setOnClickListener(v -> {
            if (mediaPlayer != null && isAudioPrepared) {
                mediaPlayer.stop();
                isAudioPrepared = false;
                seekBarAudio.setProgress(0);
                tvCurrentTime.setText("0:00");
                tvAudioStatus.setText("Status: ⏹ Stopped");
            }
        });

        // Restart
        btnAudioRestart.setOnClickListener(v -> {
            if (audioUri == null) {
                Toast.makeText(this, "Open an audio file first", Toast.LENGTH_SHORT).show();
                return;
            }
            resetAudioPlayer();
            prepareAndPlay();
            tvAudioStatus.setText("Status: ↺ Restarted");
        });

        // SeekBar drag
        seekBarAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isAudioPrepared && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    tvCurrentTime.setText(formatTime(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    // =========================================================================
    //  AUDIO HELPERS
    // =========================================================================

    private void checkPermissionAndPickFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_AUDIO
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                audioPickerLauncher.launch("audio/*");
            } else {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_AUDIO});
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                audioPickerLauncher.launch("audio/*");
            } else {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
            }
        }
    }

    private void prepareAndPlay() {
        if (audioUri == null) return;

        resetAudioPlayer();
        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(this, audioUri);
            mediaPlayer.prepareAsync();
            tvAudioStatus.setText("Status: Buffering…");

            mediaPlayer.setOnPreparedListener(mp -> {
                isAudioPrepared = true;
                mp.start();
                seekBarAudio.setMax(mp.getDuration());
                tvTotalTime.setText(formatTime(mp.getDuration()));
                tvAudioStatus.setText("Status: ▶ Playing");
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                tvAudioStatus.setText("Status: ✅ Playback complete");
                seekBarAudio.setProgress(0);
                tvCurrentTime.setText("0:00");
                isAudioPrepared = false;
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                tvAudioStatus.setText("Status: ❌ Error playing file");
                isAudioPrepared = false;
                return true;
            });

        } catch (IOException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            tvAudioStatus.setText("Status: ❌ Failed to load");
        }
    }

    private void resetAudioPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isAudioPrepared = false;
        seekBarAudio.setProgress(0);
        tvCurrentTime.setText("0:00");
        tvTotalTime.setText("0:00");
    }

    private String formatTime(int ms) {
        int totalSec = ms / 1000;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format("%d:%02d", min, sec);
    }

    // =========================================================================
    //  LIFECYCLE
    // =========================================================================

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView.isPlaying()) videoView.pause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        seekHandler.removeCallbacks(seekUpdater);
        if (videoView != null) videoView.stopPlayback();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
