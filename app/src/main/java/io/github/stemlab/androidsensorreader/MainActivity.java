package io.github.stemlab.androidsensorreader;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.sails.engine.SAILS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.github.stemlab.androidsensorreader.pojo.Location;
import io.github.stemlab.androidsensorreader.pojo.Signal;
import io.github.stemlab.androidsensorreader.utils.CSVUtils;

//import com.indooratlas.android.sdk.IALocation;
//import com.indooratlas.android.sdk.IALocationListener;
//import com.indooratlas.android.sdk.IALocationManager;
//import com.indooratlas.android.sdk.IALocationRequest;

public class MainActivity extends AppCompatActivity implements SensorEventListener,AdapterView.OnItemSelectedListener {

    static SAILS mSails;
    private static int MAX_GRAPH_SAMPLES = 250; // with 50 HZ store last 5 sec
    private static String SAVED_ID = "hhar_id";
    private final int CODE_PERMISSIONS = 1;
    private TensorFlowClassifier classifier;
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
    private TextView locCaptionTextView;
    private Button accButton;
    private boolean isPressed = false;
    //private long accBaseMillisec = -1L, gyrBaseMillisec = -1L;
    //private long accSamplesPerSec = 0, gyrSamplesPerSec = 0;
    private long baseMillisec = -1L;
    private String rateCaption;
    private String defValue;
    private String buttonStartCaption;
    private String buttonStopCaption;
    private File sensorDataFile;
    private File locDataFile;
    private String DataFileTemplate = "_sample_";
    private File sensorDataDirectory;
    private File locDataDirectory;
    private LineGraphSeries<DataPoint> accXSeries;
    private LineGraphSeries<DataPoint> accYSeries;
    private LineGraphSeries<DataPoint> accZSeries;
    private LineGraphSeries<DataPoint> gyrXSeries;
    private LineGraphSeries<DataPoint> gyrYSeries;
    private LineGraphSeries<DataPoint> gyrZSeries;
    private int lastAccSample = 0;
    private int lastGyrSample = 0;
    private List<Signal> accelerometerSamples;
    private List<Location> locationSamples;
    private List<Signal> gyroscopeSamples;
    private Spinner actionSpinner;
    private Spinner userSpinner;
    private Spinner roomSpinner;
    private String currentAction;
    private String currentUser;
    private String currentRoom;
    //private IALocationManager mIALocationManager;
    private Location lastLocation;
    private SharedPreferences sPref;
    private static String TOKEN = "8a5e3d1c9e664d43a25daba99348aee2";
    private static String BUILDING_ID = "53f33cd1f45da9105f000931";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String[] neededPermissions = {
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions( this, neededPermissions, CODE_PERMISSIONS);


        //mIALocationManager = IALocationManager.create(this);
        //mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener);
        lastLocation = new Location();
        locationSamples = new ArrayList<>();

        accX = findViewById(R.id.acc_x_data);
        accY = findViewById(R.id.acc_y_data);
        accZ = findViewById(R.id.acc_z_data);
        gyrX = findViewById(R.id.gyr_x_data);
        gyrY = findViewById(R.id.gyr_y_data);
        gyrZ = findViewById(R.id.gyr_z_data);

        locCaptionTextView = findViewById(R.id.loc_cap_text_view);
        accButton = findViewById(R.id.sensor_start);

        rateCaption = getString(R.string.loc_cap);
        defValue = getString(R.string.initial_values);
        buttonStartCaption = getString(R.string.button_start);
        buttonStopCaption = getString(R.string.button_stop);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorDataDirectory = getApplicationContext().getDir("sensor", Context.MODE_PRIVATE);
        locDataDirectory = getApplicationContext().getDir("location", Context.MODE_PRIVATE);

        accGraph = findViewById(R.id.acc_graph);
        gyrGraph = findViewById(R.id.gyr_graph);

        classifier = new TensorFlowClassifier(getApplicationContext());
        accelerometerSamples = new ArrayList<>();
        gyroscopeSamples = new ArrayList<>();

        locCaptionTextView.setText(String.format(rateCaption, 0.0, 0.0, 0.0));



        actionSpinner = findViewById(R.id.actions_spinner);
        actionSpinner.setOnItemSelectedListener(this);
        userSpinner = findViewById(R.id.user_spinner);
        userSpinner.setOnItemSelectedListener(this);
        roomSpinner = findViewById(R.id.room_spinner);
        roomSpinner.setOnItemSelectedListener(this);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> actionAdapter = ArrayAdapter.createFromResource(this,
                R.array.actions_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        actionSpinner.setAdapter(actionAdapter);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> userAdapter = ArrayAdapter.createFromResource(this,
                R.array.user_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        userSpinner.setAdapter(userAdapter);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> roomAdapter = ArrayAdapter.createFromResource(this,
                R.array.room_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        roomSpinner.setAdapter(roomAdapter);


        //SAILS BUILDNGO
        //new a SAILS engine.
        mSails = new SAILS(this);
        //set location mode.
        mSails.setMode(SAILS.WIFI_GFP_IMU);
        //set floor number sort rule from descending to ascending.
        mSails.setReverseFloorList(true);

        mSails.loadCloudBuilding("TOKEN", "BUILDING_ID", new SAILS.OnFinishCallback() {
            @Override
            public void onSuccess(String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast t = Toast.makeText(getBaseContext(), "Building successfully loaded", Toast.LENGTH_SHORT);
                        t.show();
                    }
                });
                mSails.startLocatingEngine();
            }

            @Override
            public void onFailed(String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast t = Toast.makeText(getBaseContext(), "Fail on load building", Toast.LENGTH_SHORT);
                        t.show();
                    }
                });
            }
        });

        //create location change call back.
        mSails.setOnLocationChangeEventListener(new SAILS.OnLocationChangeEventListener() {
            @Override
            public void OnLocationChange() {
                if (mSails.isLocationEngineStarted() && mSails.isLocationFix()) {
                    lastLocation = new Location(mSails.getLatitude(), mSails.getLongitude(), mSails.getAccuracy());
                    locCaptionTextView.setText(String.format(rateCaption, lastLocation.getLatitude(), lastLocation.getLongitude(), lastLocation.getAccuracy()));
                    if(isPressed){
                        locationSamples.add(lastLocation);
                    }
                }
            }
        });

        setupGraph();
        setupButtons();
    }

    /*private IALocationListener mIALocationListener = new IALocationListener() {

        // Called when the location has changed.
        @Override
        public void onLocationChanged(IALocation location) {
                lastLocation = new Location(location.getLatitude(), location.getLongitude(), location.getAccuracy());
                locCaptionTextView.setText(String.format(rateCaption, lastLocation.getLatitude(), lastLocation.getLongitude(), lastLocation.getAccuracy()));
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }
    };*/

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //Handle if any of the permissions are denied, in grantResults
    }

    //invoke repeatedly when device is in motion
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;

        /*if(sensor.getType() == Sensor.TYPE_ACCELEROMETER || sensor.getType() == Sensor.TYPE_GYROSCOPE){
            locationSamples.put(sensorEvent.timestamp, lastLocation);
        }*/
        //activityPrediction();
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            updateTextAndGraphValues(sensorEvent.values, sensor.getType());

/*
            try {
                writeLine(accDataFile, sensorEvent.values);
            } catch (IOException e) {
                e.printStackTrace();
            }
*/

            /*long currentMillisec = System.currentTimeMillis();
            if (accBaseMillisec < 0) {
                accBaseMillisec = currentMillisec;
                accSamplesPerSec = 0;
            } else if ((currentMillisec - accBaseMillisec) < 1000L) {
                ++accSamplesPerSec;
            } else {
                rateCaptionTextView.setText(String.format(rateCaption, accSamplesPerSec, gyrSamplesPerSec));
                accSamplesPerSec = 1;
                accBaseMillisec = currentMillisec;
            }*/

            accelerometerSamples.add(new Signal(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2], sensorEvent.timestamp));

        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {

            updateTextAndGraphValues(sensorEvent.values, sensor.getType());

/*            try {
                writeLine(gyrDataFile, sensorEvent.values);
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            /*long currentMillisec = System.currentTimeMillis();

            if (gyrBaseMillisec < 0) {
                gyrBaseMillisec = currentMillisec;
                gyrSamplesPerSec = 0;
            } else if ((currentMillisec - gyrBaseMillisec) < 1000L) {
                ++gyrSamplesPerSec;
            } else {
                rateCaptionTextView.setText(String.format(rateCaption, accSamplesPerSec, gyrSamplesPerSec));
                gyrSamplesPerSec = 1;
                gyrBaseMillisec = currentMillisec;
            }*/

            gyroscopeSamples.add(new Signal(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2], sensorEvent.timestamp));
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
        //mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener);
    }

    protected void onPause() {
        super.onPause();
        isPressed = false;
        baseMillisec = -1L;
        unRegisterSensorListener();
        //mIALocationManager.removeLocationUpdates(mIALocationListener);
    }

    private void setupButtons() {
        accButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPressed) {
                    isPressed = false;
                    unRegisterSensorListener();
                    baseMillisec = -1L;
                    writePairedSignal();
                } else {
                    isPressed = true;
                    registerSensorListener();
                    int counter = getFileCounter();
                    sensorDataFile = new File(sensorDataDirectory, currentAction + "_" + currentUser + "_" + currentRoom + DataFileTemplate + counter);
                    locDataFile = new File(locDataDirectory, currentAction + "_" + currentUser + "_" + currentRoom + DataFileTemplate + counter);
                    ///fileCounter++;
                    System.out.println(currentAction + DataFileTemplate + counter);
                    setFileCounter(counter + 1);
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
        //rateCaptionTextView.setText(String.format(rateCaption, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0));
    }

    public void registerSensorListener() {
        //SENSOR_DELAY_GAME = 50HZ
        //SENSOR_DELAY_UI = 17HZ
        //SENSOR_DELAY_FASTEST = 500hz
        //SENSOR_DELAY_NORMAL = 6Hz
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

    private void writePairedSignal() {


        /*long currentMillisec = System.currentTimeMillis();

        if (baseMillisec < 0) {
            baseMillisec = currentMillisec;
        } else if ((currentMillisec - baseMillisec) >= 5000L) {
            baseMillisec = currentMillisec;*/
        //float[] results = classifier.predictProbabilities(toFloatArray(D));
        new SaveFileTask().execute(accelerometerSamples, gyroscopeSamples, locationSamples);

        //}
    }

    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (adapterView.getId()) {
            case R.id.actions_spinner:
                currentAction = (String) adapterView.getItemAtPosition(i);
                break;
            case R.id.room_spinner:
                currentRoom = (String) adapterView.getItemAtPosition(i);
                break;
            case R.id.user_spinner:
                currentUser = (String) adapterView.getItemAtPosition(i);
                break;
            default:
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    int getFileCounter() {
        sPref = getPreferences(MODE_PRIVATE);
        int defaultValue = getResources().getInteger(R.integer.def_counter);
        int count = sPref.getInt(SAVED_ID, defaultValue);
        return count;
    }

    void setFileCounter(int count) {
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putInt(SAVED_ID, count);
        ed.commit();
    }

    private class SaveFileTask extends AsyncTask<Object, Void, String> {

        @Override
        protected String doInBackground(Object... objects) {

            if (!accelerometerSamples.isEmpty() && !gyroscopeSamples.isEmpty()) {
                List<HashMap> results = classifier.pairSignalsByTime(accelerometerSamples, gyroscopeSamples/*, locationSamples*/);
                try {
                    CSVUtils.writeSignal(sensorDataFile, results);
                    CSVUtils.writeLocation(locDataFile, locationSamples);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            accelerometerSamples.clear();
            gyroscopeSamples.clear();
            locationSamples.clear();

            return sensorDataFile.getName();
        }

        protected void onPostExecute(String result) {
            Toast t = Toast.makeText(getBaseContext(), "Saved: " + result, Toast.LENGTH_SHORT);
            t.show();
        }
    }
}
