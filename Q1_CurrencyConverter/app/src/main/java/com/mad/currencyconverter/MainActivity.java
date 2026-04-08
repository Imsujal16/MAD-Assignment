package com.mad.currencyconverter;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final DecimalFormat RESULT_FORMAT = new DecimalFormat("#,##0.00");

    private final Map<String, Double> currencyRates = new LinkedHashMap<>();

    private EditText amountInput;
    private AutoCompleteTextView fromCurrencyDropdown;
    private AutoCompleteTextView toCurrencyDropdown;
    private TextView resultValue;
    private TextView resultSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyStoredTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupRates();
        bindViews();
        setupCurrencyDropdowns();
        setupClicks();
        convertAmount();
    }

    private void bindViews() {
        amountInput = findViewById(R.id.amountInput);
        fromCurrencyDropdown = findViewById(R.id.fromCurrencyDropdown);
        toCurrencyDropdown = findViewById(R.id.toCurrencyDropdown);
        resultValue = findViewById(R.id.resultValue);
        resultSummary = findViewById(R.id.resultSummary);
    }

    private void setupRates() {
        currencyRates.put("USD", 1.00);
        currencyRates.put("INR", 83.12);
        currencyRates.put("JPY", 151.44);
        currencyRates.put("EUR", 0.92);
    }

    private void setupCurrencyDropdowns() {
        String[] currencies = currencyRates.keySet().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, currencies);

        fromCurrencyDropdown.setAdapter(adapter);
        toCurrencyDropdown.setAdapter(adapter);

        fromCurrencyDropdown.setText("INR", false);
        toCurrencyDropdown.setText("USD", false);
    }

    private void setupClicks() {
        MaterialButton convertButton = findViewById(R.id.convertButton);
        MaterialButton swapButton = findViewById(R.id.swapButton);
        ImageButton settingsButton = findViewById(R.id.settingsButton);

        convertButton.setOnClickListener(view -> convertAmount());
        swapButton.setOnClickListener(view -> swapCurrencies());
        settingsButton.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class))
        );

        fromCurrencyDropdown.setOnItemClickListener((parent, view, position, id) -> convertAmount());
        toCurrencyDropdown.setOnItemClickListener((parent, view, position, id) -> convertAmount());
    }

    private void swapCurrencies() {
        CharSequence from = fromCurrencyDropdown.getText();
        CharSequence to = toCurrencyDropdown.getText();

        fromCurrencyDropdown.setText(to, false);
        toCurrencyDropdown.setText(from, false);
        convertAmount();
    }

    private void convertAmount() {
        String amountText = amountInput.getText().toString().trim();
        if (TextUtils.isEmpty(amountText)) {
            resultValue.setText(getString(R.string.default_result_value));
            resultSummary.setText(getString(R.string.enter_amount_hint));
            return;
        }

        String fromCurrency = fromCurrencyDropdown.getText().toString().trim();
        String toCurrency = toCurrencyDropdown.getText().toString().trim();

        if (!currencyRates.containsKey(fromCurrency) || !currencyRates.containsKey(toCurrency)) {
            Toast.makeText(this, R.string.invalid_currency_selection, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            double convertedAmount = amount / currencyRates.get(fromCurrency) * currencyRates.get(toCurrency);

            resultValue.setText(String.format(Locale.US, "%s %s", RESULT_FORMAT.format(convertedAmount), toCurrency));
            resultSummary.setText(String.format(
                    Locale.US,
                    "%s %s = %s %s",
                    RESULT_FORMAT.format(amount),
                    fromCurrency,
                    RESULT_FORMAT.format(convertedAmount),
                    toCurrency
            ));
        } catch (NumberFormatException exception) {
            Toast.makeText(this, R.string.invalid_amount_message, Toast.LENGTH_SHORT).show();
        }
    }
}
