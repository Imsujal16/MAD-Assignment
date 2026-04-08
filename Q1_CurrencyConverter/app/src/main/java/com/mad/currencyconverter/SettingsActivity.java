package com.mad.currencyconverter;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyStoredTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton backButton = findViewById(R.id.backButton);
        MaterialSwitch darkModeSwitch = findViewById(R.id.darkModeSwitch);
        TextView modeLabel = findViewById(R.id.modeLabel);

        boolean isDarkMode = ThemeHelper.isDarkModeEnabled(this);
        darkModeSwitch.setChecked(isDarkMode);
        modeLabel.setText(isDarkMode ? R.string.dark_mode_on : R.string.dark_mode_off);

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            modeLabel.setText(isChecked ? R.string.dark_mode_on : R.string.dark_mode_off);
            ThemeHelper.setDarkMode(this, isChecked);
        });

        backButton.setOnClickListener(view -> finish());
    }
}
