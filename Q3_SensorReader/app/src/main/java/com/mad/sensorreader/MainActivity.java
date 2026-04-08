package com.mad.sensorreader;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor lightSensor;
    private Sensor proximitySensor;

    private TextView accelerometerValue;
    private TextView lightValue;
    private TextView proximityValue;
    private TextView accelerometerStatus;
    private TextView lightStatus;
    private TextView proximityStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        bindViews();
        showSensorAvailability();
    }

    private void bindViews() {
        accelerometerValue = findViewById(R.id.accelerometerValue);
        lightValue = findViewById(R.id.lightValue);
        proximityValue = findViewById(R.id.proximityValue);
        accelerometerStatus = findViewById(R.id.accelerometerStatus);
        lightStatus = findViewById(R.id.lightStatus);
        proximityStatus = findViewById(R.id.proximityStatus);
    }

    private void showSensorAvailability() {
        accelerometerStatus.setText(getAvailabilityText(accelerometerSensor));
        lightStatus.setText(getAvailabilityText(lightSensor));
        proximityStatus.setText(getAvailabilityText(proximitySensor));
    }

    private String getAvailabilityText(Sensor sensor) {
        return sensor == null ? getString(R.string.sensor_unavailable) : getString(R.string.sensor_ready);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSensor(accelerometerSensor);
        registerSensor(lightSensor);
        registerSensor(proximitySensor);
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    private void registerSensor(Sensor sensor) {
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValue.setText(String.format(
                    Locale.US,
                    "X: %.2f\nY: %.2f\nZ: %.2f m/s²",
                    event.values[0],
                    event.values[1],
                    event.values[2]
            ));
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            lightValue.setText(String.format(Locale.US, "%.2f lux", event.values[0]));
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distance = event.values[0];
            String state = distance <= 1.0f ? "Near" : "Far";
            proximityValue.setText(String.format(Locale.US, "%.2f cm (%s)", distance, state));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Accuracy updates are not required for the assignment UI.
    }
}
