package io.github.stemlab.androidsensorreader;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.IOException;

import static io.github.stemlab.androidsensorreader.utils.CSVUtils.writeLine;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static int MAX_GRAPH_SAMPLES = 250; // with 50 HZ store last 5 sec
    private SensorManager sensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;
    private GraphView accGraph;
    private GraphView gyrGraph;
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
    private LineGraphSeries<DataPoint> accXSeries;
    private LineGraphSeries<DataPoint> accYSeries;
    private LineGraphSeries<DataPoint> accZSeries;
    private LineGraphSeries<DataPoint> gyrXSeries;
    private LineGraphSeries<DataPoint> gyrYSeries;
    private LineGraphSeries<DataPoint> gyrZSeries;
    private int lastAccSample = 0;
    private int lastGyrSample = 0;

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

        accGraph = findViewById(R.id.acc_graph);
        gyrGraph = findViewById(R.id.gyr_graph);

        setupGraph();
        setupButtons();
    }

    //invoke repeatedly when device is in motion
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            updateTextAndGraphValues(sensorEvent.values, sensor.getType());

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

            updateTextAndGraphValues(sensorEvent.values, sensor.getType());

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

        resetTextAndGraphValues();

        updateButtons();

        sensorManager.unregisterListener(this);
    }

    public void resetTextAndGraphValues() {

        accX.setText(defValue);
        accY.setText(defValue);
        accZ.setText(defValue);
        gyrX.setText(defValue);
        gyrY.setText(defValue);
        gyrZ.setText(defValue);


        accXSeries.resetData(new DataPoint[0]);
        accYSeries.resetData(new DataPoint[0]);
        accZSeries.resetData(new DataPoint[0]);

        gyrXSeries.resetData(new DataPoint[0]);
        gyrYSeries.resetData(new DataPoint[0]);
        gyrZSeries.resetData(new DataPoint[0]);

        lastAccSample = 0;
        lastGyrSample = 0;
    }

    private void setupGraph() {

        ///Accelerometer graph
        accXSeries = new LineGraphSeries<>();
        accYSeries = new LineGraphSeries<>();
        accZSeries = new LineGraphSeries<>();
        accGraph.addSeries(accXSeries);
        accGraph.addSeries(accYSeries);
        accGraph.addSeries(accZSeries);

        accXSeries.setColor(Color.RED);
        accYSeries.setColor(Color.BLUE);
        accZSeries.setColor(Color.GREEN);

        ///Gyroscope graph
        gyrXSeries = new LineGraphSeries<>();
        gyrYSeries = new LineGraphSeries<>();
        gyrZSeries = new LineGraphSeries<>();
        gyrGraph.addSeries(gyrXSeries);
        gyrGraph.addSeries(gyrYSeries);
        gyrGraph.addSeries(gyrZSeries);

        gyrXSeries.setColor(Color.RED);
        gyrYSeries.setColor(Color.BLUE);
        gyrZSeries.setColor(Color.GREEN);

        // customize a little bit viewport
        Viewport accViewport = accGraph.getViewport();
        Viewport gyrViewport = gyrGraph.getViewport();
        //viewport.setYAxisBoundsManual(true);
        //viewport.setMinY(-10);
        //viewport.setMaxY(10);
        accViewport.setScrollable(true);
        gyrViewport.setScrollable(true);

    }

    private void updateTextAndGraphValues(float[] values, int sensorType) {
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {

            accX.setText(String.valueOf(values[0]));
            accY.setText(String.valueOf(values[1]));
            accZ.setText(String.valueOf(values[2]));

            accXSeries.appendData(new DataPoint(lastAccSample++, values[0]), false, MAX_GRAPH_SAMPLES);
            accYSeries.appendData(new DataPoint(lastAccSample++, values[1]), false, MAX_GRAPH_SAMPLES);
            accZSeries.appendData(new DataPoint(lastAccSample++, values[2]), false, MAX_GRAPH_SAMPLES);

        } else if (sensorType == Sensor.TYPE_GYROSCOPE) {

            gyrX.setText(String.valueOf(values[0]));
            gyrY.setText(String.valueOf(values[1]));
            gyrZ.setText(String.valueOf(values[2]));

            gyrXSeries.appendData(new DataPoint(lastGyrSample++, values[0]), false, MAX_GRAPH_SAMPLES);
            gyrYSeries.appendData(new DataPoint(lastGyrSample++, values[1]), false, MAX_GRAPH_SAMPLES);
            gyrZSeries.appendData(new DataPoint(lastGyrSample++, values[2]), false, MAX_GRAPH_SAMPLES);

        } else {
            // unkown sensor type;
            // TODO: throw exception unknown sensor
        }
    }
}
