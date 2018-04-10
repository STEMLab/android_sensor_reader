package io.github.stemlab.androidsensorreader;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import static io.github.stemlab.androidsensorreader.utils.CSVUtils.writeLine;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;

    private TextView accX;
    private TextView accY;
    private TextView accZ;
    private TextView gyrX;
    private TextView gyrY;
    private TextView gyrZ;
    private TextView rateCaptionTextView;
    private Button accButton;

    private boolean isPressed = false;
    private long accBaseMillisec = -1L, gyrBaseMillisec = -1L;
    private long accSamplesPerSec = 0, gyrSamplesPerSec = 0;

    private String rateCaption;
    private String defValue;
    private String buttonStartCaption;
    private String buttonStopCaption;
    private File accDataFile;
    private String accDataFileTemplate = "acc_sample_";
    private File gyrDataFile;
    private String gyrDataFileTemplate = "gyr_sample_";
    private int fileCounter = 1;
    private File dataDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accX = findViewById(R.id.acc_x_data);
        accY = findViewById(R.id.acc_y_data);
        accZ = findViewById(R.id.acc_z_data);
        gyrX = findViewById(R.id.gyr_x_data);
        gyrY = findViewById(R.id.gyr_y_data);
        gyrZ = findViewById(R.id.gyr_z_data);

        rateCaptionTextView = findViewById(R.id.rate_cap_text_view);
        accButton = findViewById(R.id.acc_button);

        rateCaption = getString(R.string.rate_cap);
        defValue = getString(R.string.initial_values);
        buttonStartCaption = getString(R.string.button_start);
        buttonStopCaption = getString(R.string.button_stop);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        dataDirectory = getApplicationContext().getDir("SensorData", Context.MODE_PRIVATE);

        setupButtons();
    }

    //invoke repeatedly when device is in motion
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            accX.setText(String.valueOf(sensorEvent.values[0]));
            accY.setText(String.valueOf(sensorEvent.values[1]));
            accZ.setText(String.valueOf(sensorEvent.values[2]));

            try {
                writeLine(accDataFile, sensorEvent.values);
            } catch (IOException e) {
                e.printStackTrace();
            }

            long currentMillisec = System.currentTimeMillis();
            if (accBaseMillisec < 0) {
                accBaseMillisec = currentMillisec;
                accSamplesPerSec = 0;
            } else if ((currentMillisec - accBaseMillisec) < 1000L) {
                ++accSamplesPerSec;
            } else {
                rateCaptionTextView.setText(String.format(rateCaption, accSamplesPerSec, gyrSamplesPerSec));
                accSamplesPerSec = 1;
                accBaseMillisec = currentMillisec;
            }
        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {

            gyrX.setText(String.valueOf(sensorEvent.values[0]));
            gyrY.setText(String.valueOf(sensorEvent.values[1]));
            gyrZ.setText(String.valueOf(sensorEvent.values[2]));

            try {
                writeLine(gyrDataFile, sensorEvent.values);
            } catch (IOException e) {
                e.printStackTrace();
            }

            long currentMillisec = System.currentTimeMillis();

            if (gyrBaseMillisec < 0) {
                gyrBaseMillisec = currentMillisec;
                gyrSamplesPerSec = 0;
            } else if ((currentMillisec - gyrBaseMillisec) < 1000L) {
                ++gyrSamplesPerSec;
            } else {
                rateCaptionTextView.setText(String.format(rateCaption, accSamplesPerSec, gyrSamplesPerSec));
                gyrSamplesPerSec = 1;
                gyrBaseMillisec = currentMillisec;
            }
        } else {
            // unkown sensor type;
            // TODO: throw exception unknown sensor
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // when accuracy of sensor is changed this method is triggered
    }

    protected void onResume() {
        super.onResume();
        updateButtons();
    }

    protected void onPause() {
        super.onPause();
        unRegisterSensorListener();
    }

    private void setupButtons() {
        accButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPressed) {
                    isPressed = false;
                    unRegisterSensorListener();
                } else {
                    isPressed = true;
                    registerSensorListener();
                    accDataFile = new File(dataDirectory, accDataFileTemplate + fileCounter);
                    gyrDataFile = new File(dataDirectory, gyrDataFileTemplate + fileCounter);
                    fileCounter++;
                }
            }
        });
    }

    private void updateButtons() {
        if (isPressed) {
            accButton.setText(buttonStopCaption);
        } else {
            accButton.setText(buttonStartCaption);
        }
        rateCaptionTextView.setText(String.format(rateCaption, 0, 0));
    }

    public void registerSensorListener() {
        sensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_GAME);
        updateButtons();
    }

    public void unRegisterSensorListener() {

        accX.setText(defValue);
        accY.setText(defValue);
        accZ.setText(defValue);
        gyrX.setText(defValue);
        gyrY.setText(defValue);
        gyrZ.setText(defValue);

        updateButtons();

        sensorManager.unregisterListener(this);
    }
}
